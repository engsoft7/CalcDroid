#!/usr/bin/env python3
"""Sincroniza a ficha da Play Store com os arquivos de play-store/listing/.

Estrutura (uma pasta por idioma da ficha, com o código usado pela Play):

  play-store/listing/
    pt-BR/
      title.txt                 nome do app (até 30 caracteres)
      short_description.txt     descrição curta (até 80)
      full_description.txt      descrição completa (até 4000)
      images/
        icon.png                512x512
        featureGraphic.png      1024x500 (sem transparência)
        phoneScreenshots/       2 a 8 capturas, enviadas em ordem alfabética
          01-basica.png ...

Só o que existe aqui é enviado: um texto ou imagem ausente continua como está
no Play Console. Uma pasta de capturas vazia (ou ausente) não apaga as
capturas que já estão na loja.

Comandos:
  check     confere tamanhos de texto e formatos de imagem (sem rede)
  validate  manda tudo para uma edição na Play e pede a validação, sem publicar
  publish   manda tudo e publica (a Play pode revisar antes de ir ao ar)
  download  baixa a ficha atual da Play para --out, no mesmo formato

Os comandos que falam com a Play leem a chave JSON da conta de serviço da
variável de ambiente PLAY_SERVICE_ACCOUNT_JSON e precisam de:
  pip install -r tools/requirements-play.txt
"""

import argparse
import hashlib
import json
import os
import struct
import sys
import urllib.request
from pathlib import Path

PACKAGE = "io.github.engsoft7.calcdroid"
REPO = Path(__file__).resolve().parent.parent
DEFAULT_DIR = REPO / "play-store" / "listing"

# Arquivo de texto -> campo da API e limite de caracteres da Play.
TEXT_FIELDS = {
    "title.txt": ("title", 30),
    "short_description.txt": ("shortDescription", 80),
    "full_description.txt": ("fullDescription", 4000),
}
# Imagens únicas: tipo -> (largura, altura) exigida, ou None se livre.
SINGLE_IMAGES = {"icon": (512, 512), "featureGraphic": (1024, 500), "tvBanner": (1280, 720)}
SCREENSHOT_TYPES = [
    "phoneScreenshots",
    "sevenInchScreenshots",
    "tenInchScreenshots",
    "tvScreenshots",
    "wearScreenshots",
]
IMAGE_EXTENSIONS = (".png", ".jpg", ".jpeg")
MB = 1024 * 1024


class ListingError(Exception):
    pass


# --------------------------------------------------------------------------
# Leitura local
# --------------------------------------------------------------------------

def read_text(path):
    # Sem as quebras de linha das pontas; as de dentro do texto ficam, porque
    # a Play mostra cada uma delas.
    return path.read_text(encoding="utf-8").strip()


def languages(root):
    if not root.is_dir():
        raise ListingError(f"Pasta {root} não encontrada.")
    langs = sorted(p.name for p in root.iterdir() if p.is_dir() and not p.name.startswith("."))
    if not langs:
        raise ListingError(f"Nenhuma pasta de idioma em {root} (ex.: {root / 'pt-BR'}).")
    return langs


def find_single_image(lang_dir, image_type):
    for ext in IMAGE_EXTENSIONS:
        path = lang_dir / "images" / f"{image_type}{ext}"
        if path.is_file():
            return path
    return None


def find_screenshots(lang_dir, image_type):
    folder = lang_dir / "images" / image_type
    if not folder.is_dir():
        return []
    return sorted(p for p in folder.iterdir() if p.suffix.lower() in IMAGE_EXTENSIONS)


def image_info(path):
    """(formato, largura, altura, tem_transparência) lidos do cabeçalho."""
    data = path.read_bytes()
    if data[:8] == b"\x89PNG\r\n\x1a\n":
        width, height, _depth, color = struct.unpack(">IIBB", data[16:26])
        alpha = color in (4, 6)
        # Percorre os chunks até os dados da imagem procurando tRNS
        # (transparência em PNG sem canal alfa).
        pos = 8
        while pos + 8 <= len(data):
            length = struct.unpack(">I", data[pos:pos + 4])[0]
            kind = data[pos + 4:pos + 8]
            if kind == b"tRNS":
                alpha = True
            if kind in (b"IDAT", b"IEND"):
                break
            pos += 12 + length
        return "png", width, height, alpha
    if data[:2] == b"\xff\xd8":
        pos = 2
        sof = {0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF}
        while pos + 4 <= len(data):
            if data[pos] != 0xFF:
                pos += 1
                continue
            marker = data[pos + 1]
            if marker == 0xFF:
                pos += 1
                continue
            if marker in (0x01, 0xD8) or 0xD0 <= marker <= 0xD7:
                pos += 2
                continue
            length = struct.unpack(">H", data[pos + 2:pos + 4])[0]
            if marker in sof:
                height, width = struct.unpack(">HH", data[pos + 5:pos + 9])
                return "jpeg", width, height, False
            pos += 2 + length
    raise ListingError(f"{path}: não é um PNG nem um JPEG válido.")


def check_image(path, image_type, problems):
    rel = path.relative_to(REPO) if path.is_relative_to(REPO) else path
    try:
        fmt, width, height, alpha = image_info(path)
    except ListingError as e:
        problems.append(str(e))
        return
    size = path.stat().st_size
    if image_type in SINGLE_IMAGES:
        expected = SINGLE_IMAGES[image_type]
        if (width, height) != expected:
            problems.append(f"{rel}: {width}x{height}, mas a Play exige {expected[0]}x{expected[1]}.")
        if image_type == "icon":
            if fmt != "png":
                problems.append(f"{rel}: o ícone precisa ser PNG.")
            if size > 1 * MB:
                problems.append(f"{rel}: {size // 1024} KB; o ícone pode ter no máximo 1 MB.")
        else:
            if alpha:
                problems.append(f"{rel}: tem transparência; a Play exige imagem sem canal alfa.")
            if size > 15 * MB:
                problems.append(f"{rel}: maior que 15 MB.")
    else:
        short, long_ = sorted((width, height))
        if short < 320 or long_ > 3840:
            problems.append(f"{rel}: {width}x{height}; cada lado precisa ter entre 320 e 3840 px.")
        if long_ > 2 * short:
            problems.append(f"{rel}: {width}x{height}; o lado maior pode ter no máximo o dobro do menor.")
        if alpha:
            problems.append(f"{rel}: tem transparência; a Play exige captura sem canal alfa.")
        if size > 8 * MB:
            problems.append(f"{rel}: maior que 8 MB.")


def check(root):
    """Confere a pasta inteira; devolve a lista de problemas encontrados."""
    problems = []
    for lang in languages(root):
        lang_dir = root / lang
        for filename, (_field, limit) in TEXT_FIELDS.items():
            path = lang_dir / filename
            if path.is_file():
                text = read_text(path)
                if not text:
                    problems.append(f"{lang}/{filename}: está vazio.")
                elif len(text) > limit:
                    problems.append(f"{lang}/{filename}: {len(text)} caracteres (máximo {limit}).")
        for image_type in SINGLE_IMAGES:
            path = find_single_image(lang_dir, image_type)
            if path:
                check_image(path, image_type, problems)
        for image_type in SCREENSHOT_TYPES:
            shots = find_screenshots(lang_dir, image_type)
            if not shots:
                continue
            if len(shots) > 8:
                problems.append(f"{lang}/images/{image_type}: {len(shots)} imagens (máximo 8).")
            if image_type == "phoneScreenshots" and len(shots) < 2:
                problems.append(f"{lang}/images/{image_type}: a Play exige pelo menos 2 capturas de celular.")
            for path in shots:
                check_image(path, image_type, problems)
    return problems


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


# --------------------------------------------------------------------------
# Play Developer API
# --------------------------------------------------------------------------

def play_service():
    raw = os.environ.get("PLAY_SERVICE_ACCOUNT_JSON", "")
    if not raw.strip():
        raise ListingError("Variável PLAY_SERVICE_ACCOUNT_JSON vazia (chave JSON da conta de serviço).")
    try:
        info = json.loads(raw)
    except json.JSONDecodeError:
        raise ListingError("PLAY_SERVICE_ACCOUNT_JSON não é um JSON válido.") from None
    if info.get("type") != "service_account":
        raise ListingError("PLAY_SERVICE_ACCOUNT_JSON não é a chave de uma conta de serviço.")
    try:
        from google.oauth2 import service_account
        from googleapiclient.discovery import build
    except ImportError:
        raise ListingError("Instale as dependências: pip install -r tools/requirements-play.txt") from None
    creds = service_account.Credentials.from_service_account_info(
        info, scopes=["https://www.googleapis.com/auth/androidpublisher"]
    )
    return build("androidpublisher", "v3", credentials=creds, cache_discovery=False)


def run(request):
    return request.execute(num_retries=3)


class Edit:
    """Uma edição da Play: nada vai ao ar até o commit; descartada no fim se não for publicada."""

    def __init__(self, service, package):
        self.edits = service.edits()
        self.package = package
        self.id = run(self.edits.insert(packageName=package, body={}))["id"]
        self.closed = False

    def kw(self, **extra):
        return dict(packageName=self.package, editId=self.id, **extra)

    def validate(self):
        run(self.edits.validate(**self.kw()))

    def commit(self):
        run(self.edits.commit(**self.kw()))
        self.closed = True

    def discard(self):
        if not self.closed:
            try:
                run(self.edits.delete(**self.kw()))
            except Exception:  # a edição expira sozinha de qualquer jeito
                pass
            self.closed = True

    def listings(self):
        result = run(self.edits.listings().list(**self.kw()))
        return {item["language"]: item for item in result.get("listings", [])}

    def default_language(self):
        return run(self.edits.details().get(**self.kw())).get("defaultLanguage")

    def images(self, language, image_type):
        result = run(self.edits.images().list(**self.kw(language=language, imageType=image_type)))
        return result.get("images", [])

    def upload_image(self, language, image_type, path):
        from googleapiclient.http import MediaFileUpload

        mime = "image/png" if path.suffix.lower() == ".png" else "image/jpeg"
        media = MediaFileUpload(str(path), mimetype=mime, resumable=False)
        run(self.edits.images().upload(**self.kw(language=language, imageType=image_type), media_body=media))

    def delete_images(self, language, image_type):
        run(self.edits.images().deleteall(**self.kw(language=language, imageType=image_type)))

    def update_listing(self, language, body):
        run(self.edits.listings().update(**self.kw(language=language), body=body))


def sync(edit, root):
    """Aplica a pasta local na edição; devolve quantas mudanças foram feitas."""
    local_langs = languages(root)
    remote = edit.listings()
    default = edit.default_language()
    print(f"Idioma padrão da ficha no Play Console: {default}")
    print(f"Idiomas na Play: {', '.join(sorted(remote)) or '(nenhum)'}")
    if not any(lang in remote for lang in local_langs):
        # Quase sempre é a pasta com o nome errado: publicar criaria uma
        # tradução nova em vez de atualizar a ficha existente.
        raise ListingError(
            f"Nenhuma pasta em {root} corresponde a um idioma da ficha na Play "
            f"(pastas: {', '.join(local_langs)}). Renomeie a pasta para o código do idioma "
            f"da ficha, provavelmente '{default}'."
        )
    changes = 0
    for lang in local_langs:
        lang_dir = root / lang
        print(f"\n[{lang}]")
        current = remote.get(lang)
        if current is None:
            print("  idioma novo: será criado como tradução da ficha.")

        # Textos
        wanted = {}
        for filename, (field, _limit) in TEXT_FIELDS.items():
            path = lang_dir / filename
            if path.is_file():
                wanted[field] = read_text(path)
        changed = [f for f, v in wanted.items() if (current or {}).get(f) != v]
        if changed:
            body = {k: v for k, v in (current or {}).items() if k in ("title", "shortDescription", "fullDescription", "video")}
            body.update(wanted)
            body["language"] = lang
            edit.update_listing(lang, body)
            print(f"  textos atualizados: {', '.join(changed)}")
            changes += 1
        elif wanted:
            print("  textos: iguais aos da Play")

        # Imagens únicas (ícone, gráfico de destaque...)
        for image_type in SINGLE_IMAGES:
            path = find_single_image(lang_dir, image_type)
            if not path:
                continue
            if any(img.get("sha256") == sha256(path) for img in edit.images(lang, image_type)):
                print(f"  {image_type}: igual ao da Play")
                continue
            edit.upload_image(lang, image_type, path)
            print(f"  {image_type}: enviado ({path.name})")
            changes += 1

        # Capturas: o conjunto inteiro é trocado quando difere em algo,
        # inclusive na ordem.
        for image_type in SCREENSHOT_TYPES:
            shots = find_screenshots(lang_dir, image_type)
            if not shots:
                continue
            remote_shas = [img.get("sha256") for img in edit.images(lang, image_type)]
            if remote_shas == [sha256(p) for p in shots]:
                print(f"  {image_type}: iguais às da Play")
                continue
            edit.delete_images(lang, image_type)
            for path in shots:
                edit.upload_image(lang, image_type, path)
            print(f"  {image_type}: {len(shots)} enviadas (substituindo {len(remote_shas)})")
            changes += 1
    return changes


def download(edit, out):
    out.mkdir(parents=True, exist_ok=True)
    for lang, listing in sorted(edit.listings().items()):
        lang_dir = out / lang
        lang_dir.mkdir(parents=True, exist_ok=True)
        for filename, (field, _limit) in TEXT_FIELDS.items():
            if listing.get(field):
                (lang_dir / filename).write_text(listing[field] + "\n", encoding="utf-8")
        for image_type in list(SINGLE_IMAGES) + SCREENSHOT_TYPES:
            images = edit.images(lang, image_type)
            for index, img in enumerate(images, start=1):
                # "=s0" pede a imagem no tamanho original.
                with urllib.request.urlopen(img["url"] + "=s0", timeout=60) as response:
                    data = response.read()
                ext = ".png" if data[:8] == b"\x89PNG\r\n\x1a\n" else ".jpg"
                if image_type in SINGLE_IMAGES:
                    target = lang_dir / "images" / f"{image_type}{ext}"
                else:
                    target = lang_dir / "images" / image_type / f"{index:02d}{ext}"
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(data)
        print(f"{lang}: baixado em {lang_dir}")


def describe_http_error(error):
    try:
        detail = json.loads(error.content.decode("utf-8"))["error"]["message"]
    except Exception:
        detail = str(error)
    status = getattr(error.resp, "status", "?")
    hints = {
        401: "a chave JSON foi revogada ou é inválida.",
        403: "a conta de serviço não tem a permissão necessária no Play Console "
             "(para a ficha: 'Gerenciar presença na loja').",
        404: "o app não foi encontrado: confira o nome do pacote e se a conta de serviço tem acesso a ele.",
    }
    hint = hints.get(status)
    return f"Erro da Play API ({status}): {detail}" + (f"\nProvável causa: {hint}" if hint else "")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("command", choices=["check", "validate", "publish", "download"])
    parser.add_argument("--dir", type=Path, default=DEFAULT_DIR, help="pasta da ficha (padrão: play-store/listing)")
    parser.add_argument("--out", type=Path, default=Path("ficha-atual"), help="destino do download")
    parser.add_argument("--package", default=PACKAGE)
    args = parser.parse_args()

    try:
        if args.command != "download":
            problems = check(args.dir)
            if problems:
                for problem in problems:
                    print(f"::error::{problem}" if os.environ.get("GITHUB_ACTIONS") else f"ERRO: {problem}")
                return 1
            print(f"Ficha local OK ({', '.join(languages(args.dir))}).")
            if args.command == "check":
                return 0

        service = play_service()
        edit = Edit(service, args.package)
        try:
            if args.command == "download":
                download(edit, args.out)
                return 0
            changes = sync(edit, args.dir)
            if changes == 0:
                print("\nNada a enviar: a ficha na Play já é igual à do repositório.")
                return 0
            if args.command == "validate":
                edit.validate()
                print(f"\nValidação OK: {changes} mudança(s) aceita(s) pela Play. Nada foi publicado.")
            else:
                edit.commit()
                print(f"\nPublicado: {changes} mudança(s) enviada(s). A Play pode revisar antes de ir ao ar.")
            return 0
        finally:
            edit.discard()
    except ListingError as e:
        print(f"::error::{e}" if os.environ.get("GITHUB_ACTIONS") else f"ERRO: {e}")
        return 1
    except Exception as e:  # noqa: BLE001 - erros HTTP da API viram mensagem legível
        if type(e).__name__ == "HttpError":
            message = describe_http_error(e)
            print(f"::error::{message}" if os.environ.get("GITHUB_ACTIONS") else message)
            return 1
        raise


if __name__ == "__main__":
    sys.exit(main())

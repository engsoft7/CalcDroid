#!/usr/bin/env python3
"""Promove a versão mais recente de uma faixa da Play para outra.

Ex.: o que está no teste interno vai para produção, com as mesmas notas da
versão. Não reenvia o AAB: só aponta a faixa de destino para o versionCode
que já está na Play.

  python3 tools/play_promote.py --from internal --to production [--rollout 0.2]

--rollout entre 0 e 1 (exclusivo) faz um lançamento gradual para essa fração
dos usuários; sem ele, a versão vai para todos. Lê a chave JSON da conta de
serviço de PLAY_SERVICE_ACCOUNT_JSON (pip install -r tools/requirements-play.txt).
"""

import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import play_store_listing as listing  # noqa: E402  (reaproveita autenticação e erros)

TRACKS = ("internal", "alpha", "beta", "production")


def pick_release(track):
    """A versão ativa mais recente da faixa (maior versionCode)."""
    releases = [
        r for r in track.get("releases", [])
        if r.get("versionCodes") and r.get("status") in ("completed", "inProgress")
    ]
    if not releases:
        return None
    return max(releases, key=lambda r: max(int(v) for v in r["versionCodes"]))


def promote(service, package, source, target, rollout):
    edit = listing.Edit(service, package)
    try:
        tracks = edit.edits.tracks()
        src = listing.run(tracks.get(**edit.kw(track=source)))
        release = pick_release(src)
        if release is None:
            raise listing.ListingError(f"Nenhuma versão ativa na faixa '{source}' para promover.")
        codes = release["versionCodes"]
        print(f"Faixa '{source}': versão {release.get('name', '?')} (versionCode {', '.join(codes)})")

        dst = listing.run(tracks.get(**edit.kw(track=target)))
        current = pick_release(dst)
        if current and max(int(v) for v in current["versionCodes"]) >= max(int(v) for v in codes):
            print(f"A faixa '{target}' já tem o versionCode {', '.join(current['versionCodes'])}; nada a promover.")
            return 0

        new_release = {"versionCodes": codes, "status": "completed"}
        if release.get("name"):
            new_release["name"] = release["name"]
        if release.get("releaseNotes"):
            new_release["releaseNotes"] = release["releaseNotes"]
        if rollout is not None:
            new_release["status"] = "inProgress"
            new_release["userFraction"] = rollout
        listing.run(tracks.update(**edit.kw(track=target),
                                  body={"track": target, "releases": [new_release]}))
        edit.commit()
        alvo = f"{rollout:.0%} dos usuários" if rollout is not None else "todos os usuários"
        print(f"Promovido para '{target}' ({alvo}). A Play pode revisar antes de ir ao ar.")
        return 0
    finally:
        edit.discard()


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--from", dest="source", choices=TRACKS, default="internal")
    parser.add_argument("--to", dest="target", choices=TRACKS, default="production")
    parser.add_argument("--rollout", type=float, default=None,
                        help="fração de usuários (0 < x < 1) para lançamento gradual")
    parser.add_argument("--package", default=listing.PACKAGE)
    args = parser.parse_args()

    gha = bool(os.environ.get("GITHUB_ACTIONS"))
    if args.source == args.target:
        print("Origem e destino iguais.")
        return 1
    if args.rollout is not None and not 0 < args.rollout < 1:
        print("--rollout precisa estar entre 0 e 1 (ex.: 0.2 = 20%). Para todos, não use --rollout.")
        return 1
    try:
        return promote(listing.play_service(), args.package, args.source, args.target, args.rollout)
    except listing.ListingError as e:
        print(f"::error::{e}" if gha else f"ERRO: {e}")
        return 1
    except Exception as e:  # noqa: BLE001
        if type(e).__name__ == "HttpError":
            msg = listing.describe_http_error(e).replace(
                "(para a ficha: 'Gerenciar presença na loja')",
                "(para produção: 'Enviar para produção, excluir dispositivos e usar a Assinatura de apps')")
            print(f"::error::{msg}" if gha else msg)
            return 1
        raise


if __name__ == "__main__":
    sys.exit(main())

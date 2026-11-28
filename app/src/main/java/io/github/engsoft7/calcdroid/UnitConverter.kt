package io.github.engsoft7.calcdroid

import androidx.annotation.StringRes
import java.math.BigInteger

/**
 * Uma unidade: [toBase] leva o valor para a unidade base da categoria
 * (metro, quilograma, kelvin...) e [fromBase] traz de volta.
 */
class UnitDef(
    val id: String,
    val symbol: String,
    @param:StringRes val name: Int,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double,
)

class UnitCategory(val id: String, @param:StringRes val name: Int, val units: List<UnitDef>)

private fun linear(id: String, symbol: String, @StringRes name: Int, factor: Double) =
    UnitDef(id, symbol, name, { it * factor }, { it / factor })

object UnitConverter {

    val categories: List<UnitCategory> = listOf(
        UnitCategory(
            "length", R.string.cat_length, listOf(
                linear("m", "m", R.string.unit_meter, 1.0),
                linear("km", "km", R.string.unit_kilometer, 1000.0),
                linear("cm", "cm", R.string.unit_centimeter, 0.01),
                linear("mm", "mm", R.string.unit_millimeter, 0.001),
                linear("mi", "mi", R.string.unit_mile, 1609.344),
                linear("yd", "yd", R.string.unit_yard, 0.9144),
                linear("ft", "ft", R.string.unit_foot, 0.3048),
                linear("in", "in", R.string.unit_inch, 0.0254),
                linear("nmi", "nmi", R.string.unit_nautical_mile, 1852.0),
            )
        ),
        UnitCategory(
            "mass", R.string.cat_mass, listOf(
                linear("kg", "kg", R.string.unit_kilogram, 1.0),
                linear("g", "g", R.string.unit_gram, 0.001),
                linear("mg", "mg", R.string.unit_milligram, 1e-6),
                linear("t", "t", R.string.unit_tonne, 1000.0),
                linear("lb", "lb", R.string.unit_pound, 0.45359237),
                linear("oz", "oz", R.string.unit_ounce, 0.028349523125),
            )
        ),
        UnitCategory(
            "temperature", R.string.cat_temperature, listOf(
                UnitDef("c", "°C", R.string.unit_celsius, { it + 273.15 }, { it - 273.15 }),
                UnitDef("f", "°F", R.string.unit_fahrenheit, { (it - 32) * 5 / 9 + 273.15 }, { (it - 273.15) * 9 / 5 + 32 }),
                UnitDef("k", "K", R.string.unit_kelvin, { it }, { it }),
            )
        ),
        UnitCategory(
            "area", R.string.cat_area, listOf(
                linear("m2", "m²", R.string.unit_square_meter, 1.0),
                linear("km2", "km²", R.string.unit_square_kilometer, 1e6),
                linear("cm2", "cm²", R.string.unit_square_centimeter, 1e-4),
                linear("ha", "ha", R.string.unit_hectare, 1e4),
                linear("ac", "ac", R.string.unit_acre, 4046.8564224),
                linear("ft2", "ft²", R.string.unit_square_foot, 0.09290304),
            )
        ),
        UnitCategory(
            "volume", R.string.cat_volume, listOf(
                linear("l", "L", R.string.unit_liter, 1.0),
                linear("ml", "mL", R.string.unit_milliliter, 0.001),
                linear("m3", "m³", R.string.unit_cubic_meter, 1000.0),
                linear("gal", "gal", R.string.unit_us_gallon, 3.785411784),
                linear("floz", "fl oz", R.string.unit_us_fluid_ounce, 0.0295735295625),
                linear("cup", "", R.string.unit_cup, 0.24),
            )
        ),
        UnitCategory(
            "speed", R.string.cat_speed, listOf(
                linear("kmh", "km/h", R.string.unit_kmh, 1 / 3.6),
                linear("ms", "m/s", R.string.unit_ms, 1.0),
                linear("mph", "mph", R.string.unit_mph, 0.44704),
                linear("kn", "kn", R.string.unit_knot, 1852.0 / 3600),
            )
        ),
        UnitCategory(
            "time", R.string.cat_time, listOf(
                linear("s", "s", R.string.unit_second, 1.0),
                linear("msec", "ms", R.string.unit_millisecond, 0.001),
                linear("min", "min", R.string.unit_minute, 60.0),
                linear("h", "h", R.string.unit_hour, 3600.0),
                linear("d", "d", R.string.unit_day, 86400.0),
                linear("wk", "", R.string.unit_week, 604800.0),
            )
        ),
        UnitCategory(
            "pressure", R.string.cat_pressure, listOf(
                linear("kpa", "kPa", R.string.unit_kilopascal, 1000.0),
                linear("pa", "Pa", R.string.unit_pascal, 1.0),
                linear("bar", "bar", R.string.unit_bar, 1e5),
                linear("atm", "atm", R.string.unit_atmosphere, 101325.0),
                linear("psi", "psi", R.string.unit_psi, 6894.757293168),
                linear("mmhg", "mmHg", R.string.unit_mmhg, 133.322387415),
            )
        ),
        UnitCategory(
            "energy", R.string.cat_energy, listOf(
                linear("kj", "kJ", R.string.unit_kilojoule, 1000.0),
                linear("j", "J", R.string.unit_joule, 1.0),
                linear("kcal", "kcal", R.string.unit_kilocalorie, 4184.0),
                linear("cal", "cal", R.string.unit_calorie, 4.184),
                linear("wh", "Wh", R.string.unit_watt_hour, 3600.0),
                linear("kwh", "kWh", R.string.unit_kilowatt_hour, 3.6e6),
                linear("btu", "BTU", R.string.unit_btu, 1055.05585262),
            )
        ),
        UnitCategory(
            "power", R.string.cat_power, listOf(
                linear("kw", "kW", R.string.unit_kilowatt, 1000.0),
                linear("w", "W", R.string.unit_watt, 1.0),
                linear("hp", "hp", R.string.unit_horsepower, 745.69987158),
                linear("cv", "cv", R.string.unit_metric_horsepower, 735.49875),
                linear("btuh", "BTU/h", R.string.unit_btu_per_hour, 0.29307107),
            )
        ),
        UnitCategory(
            "angle", R.string.cat_angle, listOf(
                linear("deg", "°", R.string.unit_degree, Math.PI / 180),
                linear("rad", "rad", R.string.unit_radian, 1.0),
                linear("grad", "grad", R.string.unit_gradian, Math.PI / 200),
                linear("turn", "", R.string.unit_turn, 2 * Math.PI),
            )
        ),
        UnitCategory(
            // Base: km/L. L/100 km é o inverso (mais litros = menos km/L).
            "fuel", R.string.cat_fuel, listOf(
                linear("kml", "km/L", R.string.unit_km_per_liter, 1.0),
                UnitDef("l100", "L/100 km", R.string.unit_liters_per_100km, { 100 / it }, { 100 / it }),
                linear("mpgus", "mpg (US)", R.string.unit_mpg_us, 1.609344 / 3.785411784),
                linear("mpguk", "mpg (UK)", R.string.unit_mpg_uk, 1.609344 / 4.54609),
            )
        ),
        UnitCategory(
            "data", R.string.cat_data, listOf(
                linear("b", "bit", R.string.unit_bit, 0.125),
                linear("B", "B", R.string.unit_byte, 1.0),
                linear("KB", "KB", R.string.unit_kilobyte, 1e3),
                linear("MB", "MB", R.string.unit_megabyte, 1e6),
                linear("GB", "GB", R.string.unit_gigabyte, 1e9),
                linear("TB", "TB", R.string.unit_terabyte, 1e12),
                linear("KiB", "KiB", R.string.unit_kibibyte, 1024.0),
                linear("MiB", "MiB", R.string.unit_mebibyte, 1024.0 * 1024),
                linear("GiB", "GiB", R.string.unit_gibibyte, 1024.0 * 1024 * 1024),
            )
        ),
    )

    fun category(id: String): UnitCategory = categories.first { it.id == id }

    fun convert(value: Double, from: UnitDef, to: UnitDef): Double = to.fromBase(from.toBase(value))

    // ---- Bases numéricas (inteiros, com sinal) ----

    val bases = listOf(2, 8, 10, 16)

    fun isValidDigit(c: Char, base: Int): Boolean = Character.digit(c, base) >= 0

    /** Converte um inteiro escrito em [from] para [to]; null se o texto não for válido. */
    fun convertBase(text: String, from: Int, to: Int): String? {
        if (text.isEmpty() || text == "-") return null
        return try {
            BigInteger(text, from).toString(to).uppercase()
        } catch (e: NumberFormatException) {
            null
        }
    }
}

private fun buildTopTechText(arr: JSONArray?): String {

    if (arr == null || arr.length() == 0) {
        return "Top técnicos em retiradas\nNenhuma retirada registrada."
    }

    val sb = StringBuilder()

    sb.append("Top técnicos em retiradas\n\n")

    for (i in 0 until arr.length()) {

        val item = arr.getJSONObject(i)

        val name = item.optString("technician_name", "Técnico")
        val total = item.optString("total_out", "0")
        val last = item.optString("last_out_date", "-")

        sb.append("${i + 1}. $name — $total retirada(s)\n")
        sb.append("Última retirada: $last")

        if (i < arr.length() - 1) {
            sb.append("\n\n")
        }
    }

    return sb.toString()
}

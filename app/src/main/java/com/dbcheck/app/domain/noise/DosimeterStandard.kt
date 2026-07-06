package com.dbcheck.app.domain.noise

enum class DosimeterStandard(val preferenceValue: String) {
    NIOSH_REL("niosh_rel"),
    OSHA_PEL("osha_pel"),
    ;

    companion object {
        fun fromPreference(value: String?): DosimeterStandard =
            entries.firstOrNull { it.preferenceValue == value } ?: NIOSH_REL
    }
}

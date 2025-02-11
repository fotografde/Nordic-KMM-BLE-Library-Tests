package utils

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuidFrom

fun String.toEntaggedUuid(): Uuid {
    return uuidFrom("f001$this-f001-f001-f001-f001f001f001")
}
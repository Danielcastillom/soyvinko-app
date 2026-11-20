import re
import unicodedata


def normalize_professional_slug(value):
    """Use one canonical public profile name before validating or claiming it."""
    if not isinstance(value, str):
        return value
    value = unicodedata.normalize("NFKD", value.lower())
    value = re.sub(r"[\u0300-\u036f]", "", value)
    return re.sub(r"[^a-z0-9]+", "-", value).strip("-")

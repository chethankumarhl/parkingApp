"""Find the licence plate among all OCR text, and format it.

Plate layout:   [Region] [Class no.]      e.g. 品川 300
                [Hiragana] [Serial no.]        あ 12-34
The serial number (the big digits) is the anchor: we look at the boxes around it and
read each field separately, so logos / stickers / decorations can't break the match.
"""
import re
import unicodedata
from collections import Counter

REGION_CLASS_RE = re.compile(r"(?P<region>[\u4e00-\u9fff]{2,4}|つくば|なにわ)(?P<cls>[0-9A-Z]{2,3})")
PLATE_RE = re.compile(  # strict single-string form, used only as a last resort
    r"(?P<region>[\u4e00-\u9fff]{2,4}|つくば|なにわ)(?P<cls>[0-9A-Z]{2,3})"
    r"(?P<kana>[\u3040-\u309f])(?P<serial>[0-9・.\-]{1,6})"
)
ANCHOR_RE = re.compile(r"[\u3040-\u30ff]?[0-9・.\-]{1,8}")
KANA_RE = re.compile(r"[\u3040-\u309f]")
# ---- Hiragana that can appear on a Japanese plate ----------------------------------------
#  private cars  : さすせそ たちつてと なにぬねの はひふほ まみむめも やゆ らりるろ
#  commercial    : あいうえ かきくけこ を
#  rental cars   : わ れ          US forces: よ
# (お し へ ん are never used - too easy to confuse with 0, shi, he, n)
VALID_KANA = "あいうえかきくけこさすせそたちつてとなにぬねのはひふほまみむめもやゆよらりるれろわを"

# Characters the OCR commonly returns INSTEAD of a kana. Only used when no real kana was read.
# Katakana (ラ→ら), voiced marks (が→か) and small kana (ゃ→や) are converted automatically;
# add your own observed mistakes here, e.g. "6": "ろ".
KANA_CONFUSION = {
    "5": "ら", "S": "ら", "s": "ら",
    "0": "の", "O": "の", "o": "の",
    "1": "い", "l": "い", "I": "い", "|": "い",
    "お": "あ",
}
SMALL_KANA = {"ぁ": "あ", "ぃ": "い", "ぅ": "う", "ぇ": "え", "ゃ": "や", "ゅ": "ゆ", "ょ": "よ", "っ": "つ", "ゎ": "わ"}


def resolve_kana(text: str, lookalikes: bool = True) -> str | None:
    """Map OCR output to one valid plate hiragana, or None."""
    for ch in unicodedata.normalize("NFKC", text):
        if "\u30a1" <= ch <= "\u30f6":                 # katakana -> hiragana
            ch = chr(ord(ch) - 0x60)
        base = SMALL_KANA.get(ch, ch)
        base = unicodedata.normalize("NFD", base)[0]     # が -> か, ぱ -> は
        if base in VALID_KANA:
            return base
        if lookalikes and (ch in KANA_CONFUSION or base in KANA_CONFUSION):
            return KANA_CONFUSION.get(ch) or KANA_CONFUSION[base]
    return None


def pick_kana(items: list[dict]):
    """Best kana among OCR boxes that might hold it. Prefers real kana over look-alike characters.
    Returns (kana, box) or (None, None)."""
    for lookalikes in (False, True):
        hits = [(i["conf"], resolve_kana(normalize(i["text"]), lookalikes), i) for i in items
                if len(normalize(i["text"])) <= 3]
        hits = [h for h in hits if h[1]]
        if hits:
            _, k, box = max(hits, key=lambda h: h[0])
            return k, box
    return None, None


DIGIT_FIX = str.maketrans({"O": "0", "o": "0", "I": "1", "l": "1", "S": "5", "B": "8", "Z": "2"})


def normalize(text: str) -> str:
    text = unicodedata.normalize("NFKC", text)
    text = re.sub(r"[\s_|]+", "", text)
    for ch in "ー―—–−ｰ一":
        if ch != "一" or re.fullmatch(r"[0-9・.\-一]+", text):  # 一 only counts as a dash inside digit strings
            text = text.replace(ch, "-")
    return text.replace("·", "・").replace("•", "・")


def _fix_serial(s: str) -> str:
    s = s.translate(DIGIT_FIX).replace(".", "・")
    digits = re.sub(r"[^0-9・]", "", s)
    digits = digits[-4:] if len(digits) > 4 else digits.rjust(4, "・")
    # 4 digits show as "12-34"; shorter numbers are dot-padded: "・・12", "・・・1"
    return f"{digits[:2]}-{digits[2:]}" if "・" not in digits else digits


def parse_plate(texts: list[str]) -> dict | None:
    m = PLATE_RE.search(normalize("".join(texts)))
    if not m:
        return None
    return {"region": m["region"], "class_number": m["cls"].translate(DIGIT_FIX),
            "hiragana": m["kana"], "serial": _fix_serial(m["serial"])}


def format_registration(p: dict) -> str:
    return f"{p['region']} {p['class_number']} {p.get('hiragana') or '?'} {p['serial']}"


def _fix(text: str) -> str:
    return normalize(text).translate(DIGIT_FIX)


def find_plate(items: list[dict]) -> dict | None:
    """items: dicts with text, conf, x1, y1, x2, y2, cx, cy, h (pixel coords).
    Returns the best plate: {"parsed", "bbox", "anchor", "serial_x1", "h"} or None.
    parsed["hiragana"] is None if the kana couldn't be read."""
    best = None
    for a in items:
        na = _fix(a["text"])
        if not ANCHOR_RE.fullmatch(na) or not re.search(r"[0-9]", na):
            continue
        h = max(a["h"], 1)
        x0, x1 = a["x1"] - 2.2 * h, a["x2"] + 1.2 * h
        y0, y1 = a["y1"] - 1.8 * h, a["y2"] + 0.6 * h
        group = [i for i in items if x0 <= i["cx"] <= x1 and y0 <= i["cy"] <= y1]
        group.sort(key=lambda i: i["cx"])
        top = [i for i in group if i["cy"] < a["cy"] - 0.6 * h]
        bottom = [i for i in group if i not in top]

        # 1) serial number: big digit-only boxes on the bottom row (may be split into pieces).
        #    A leading kana (or a misread of it) glued to the digits is split off here.
        parts, kana, kana_item = [], None, None
        for i in bottom:
            n = _fix(i["text"])
            if not (ANCHOR_RE.fullmatch(n) and re.search(r"[0-9]", n) and i["h"] >= 0.6 * h):
                continue
            lead = None
            if not re.match(r"[0-9・.\-]", n[0]):                      # e.g. "ら87-62", "ラ87-62"
                lead, n = n[0], n[1:]
            elif len(re.sub(r"[^0-9]", "", n)) >= 5:                   # e.g. "587-62": a serial has max 4 digits
                lead, n = n[0], n[1:]
            if lead and kana is None:
                kana = resolve_kana(lead)
                kana_item = i if kana else None
            parts.append((i, n))
        if not parts:
            continue
        serial = _fix_serial("".join(n for _, n in parts))
        part_items = [i for i, _ in parts]

        # 2) hiragana as its own small box, left of the serial
        if kana is None:
            left = min(i["x1"] for i in part_items) + 0.3 * h
            small = [i for i in bottom if i not in part_items and i["x2"] <= left and i["h"] <= 0.85 * h]
            kana, kana_item = pick_kana(small)

        # 3) region + class number: top row first, then anything else in the window
        m = None
        for i in top:
            m = REGION_CLASS_RE.search(normalize(i["text"]))
            if m:
                break
        if not m:
            m = REGION_CLASS_RE.search("".join(normalize(i["text"]) for i in top))
        if not m:
            others = [i for i in group if i not in part_items]
            m = REGION_CLASS_RE.search("".join(normalize(i["text"]) for i in others))
        if not m:
            continue

        used = part_items + top + ([kana_item] if kana_item else [])
        cand = {
            "parsed": {"region": m["region"], "class_number": m["cls"].translate(DIGIT_FIX),
                       "hiragana": kana, "serial": serial},
            "bbox": (min(i["x1"] for i in used), min(i["y1"] for i in used),
                     max(i["x2"] for i in used), max(i["y2"] for i in used)),
            "anchor": {"cy": a["cy"], "h": h},
            "serial_x1": min(i["x1"] for i in part_items),
            "kana_raw": kana_item["text"] if kana_item else None,
            "h": h,
        }
        if best is None or h > best["h"]:
            best = cand
    return best


def vote_field(values, tiebreak=lambda v: 0):
    """Majority vote; ties go to tiebreak, then to the earliest (highest-priority) value."""
    vals = [v for v in values if v]
    if not vals:
        return None
    c = Counter(vals)
    return max(c, key=lambda v: (c[v], tiebreak(v), -vals.index(v)))


def merge_readings(parsed_list: list[dict]) -> dict:
    """Combine several readings of the same plate, field by field."""
    col = lambda k: [p[k] for p in parsed_list]
    return {
        "region": vote_field(col("region")),
        "class_number": vote_field(col("class_number")),
        "hiragana": vote_field(col("hiragana")),
        # dropped digits are the usual failure, so on a tie prefer the fuller serial
        "serial": vote_field(col("serial"), tiebreak=lambda s: -s.count("・")),
    }

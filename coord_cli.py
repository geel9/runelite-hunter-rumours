#!/usr/bin/env python3
"""Generate Java enum entries from pipe-delimited RuneScape coordinates."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


COORDINATE_RE = re.compile(
    r"x\s*:\s*(?P<x>-?\d+)\s*,\s*y\s*:\s*(?P<y>-?\d+)",
    re.IGNORECASE,
)
JAVA_IDENTIFIER_RE = re.compile(r"[A-Za-z_$][A-Za-z0-9_$]*\Z")


def parse_coordinates(value: str) -> list[tuple[int, int]]:
    """Parse coordinates such as ``|x:2362,y:3578|x:2307,y:3577``."""
    matches = list(COORDINATE_RE.finditer(value))
    if not matches:
        raise ValueError("no coordinates found; expected values such as |x:2362,y:3578")

    # Once valid coordinate tokens are removed, only separators may remain.
    remainder = COORDINATE_RE.sub("", value)
    if re.sub(r"[|;,\s]+", "", remainder):
        raise ValueError(f"invalid coordinate text near: {remainder.strip()!r}")

    return [(int(match["x"]), int(match["y"])) for match in matches]


def location_token(location: str) -> str:
    """Convert a display location to its conventional enum-name suffix."""
    return re.sub(r"[^A-Za-z0-9]+", "_", location).strip("_").upper()


def derive_creature(name: str, location: str) -> str:
    suffix = location_token(location)
    marker = f"_{suffix}" if suffix else ""
    if marker and name.upper().endswith(marker):
        creature = name[: -len(marker)]
        if creature:
            return creature
    raise ValueError(
        "creature could not be derived from name and location; pass --creature explicitly"
    )


def validate_identifier(value: str, label: str) -> None:
    if not JAVA_IDENTIFIER_RE.fullmatch(value):
        raise ValueError(f"{label} must be a valid Java identifier: {value!r}")


def java_string(value: str) -> str:
    escaped = (
        value.replace("\\", "\\\\")
        .replace('"', '\\"')
        .replace("\r", "\\r")
        .replace("\n", "\\n")
        .replace("\t", "\\t")
    )
    return f'"{escaped}"'


def generate_entries(
    name: str,
    creature: str,
    location: str,
    fairy_ring: str,
    coordinates: list[tuple[int, int]],
    plane: int = 0,
) -> str:
    validate_identifier(name, "name")
    validate_identifier(creature, "creature")

    lines = []
    for index, (x, y) in enumerate(coordinates, start=1):
        lines.append(
            f"{name}_{index}({creature}, {java_string(location)}, "
            f"{java_string(fairy_ring)}, new WorldPoint({x}, {y}, {plane})),"
        )
    return "\n".join(lines)


def read_coordinate_source(args: argparse.Namespace) -> str:
    sources = sum(
        source is not None for source in (args.coords, args.coords_file)
    ) + bool(args.stdin)
    if sources > 1:
        raise ValueError("use only one of --coords, --coords-file, or --stdin")
    if args.coords is not None:
        return args.coords
    if args.coords_file is not None:
        return args.coords_file.read_text(encoding="utf-8")
    if args.stdin:
        return sys.stdin.read()
    raise ValueError("coordinates are required; pass --coords, --coords-file, or --stdin")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Generate numbered Java WorldPoint enum entries."
    )
    parser.add_argument("--name", help="base enum name, e.g. WILD_KEBBIT_AUBURNVALE")
    parser.add_argument(
        "--creature",
        help="creature constant; derived from name and location when omitted",
    )
    parser.add_argument("--location", help="display location, e.g. Auburnvale")
    parser.add_argument("--fairy-ring", default=None, help="fairy ring code (may be empty)")
    coordinates = parser.add_mutually_exclusive_group()
    coordinates.add_argument("--coords", help="coordinate text")
    coordinates.add_argument(
        "--coords-file", type=Path, help="UTF-8 file containing coordinate text"
    )
    coordinates.add_argument(
        "--stdin", action="store_true", help="read coordinate text from standard input"
    )
    parser.add_argument("--plane", type=int, default=0, help="WorldPoint plane (default: 0)")
    parser.add_argument("--output", type=Path, help="write output to this UTF-8 file")
    return parser


def prompt_for_missing(args: argparse.Namespace) -> None:
    if args.name is None:
        args.name = input("Name: ").strip()
    if args.location is None:
        args.location = input("Location: ").strip()
    if args.creature is None:
        try:
            suggested = derive_creature(args.name, args.location)
        except ValueError:
            suggested = ""
        prompt = f"Creature [{suggested}]: " if suggested else "Creature: "
        args.creature = input(prompt).strip() or suggested
    if args.fairy_ring is None:
        args.fairy_ring = input("Fairy ring code (leave blank for none): ").strip()
    if args.coords is None and args.coords_file is None and not args.stdin:
        args.coords = input("XY coordinates: ").strip()


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)

    # With no arguments, offer a compact interactive workflow.
    if argv == [] or (argv is None and len(sys.argv) == 1):
        prompt_for_missing(args)

    try:
        if not args.name:
            raise ValueError("--name is required")
        if not args.location:
            raise ValueError("--location is required")
        if args.fairy_ring is None:
            raise ValueError("--fairy-ring is required (use an empty string for no code)")

        creature = args.creature or derive_creature(args.name, args.location)
        coordinate_text = read_coordinate_source(args)
        result = generate_entries(
            args.name,
            creature,
            args.location,
            args.fairy_ring,
            parse_coordinates(coordinate_text),
            args.plane,
        )
    except (OSError, ValueError) as error:
        parser.error(str(error))

    if args.output:
        args.output.write_text(result + "\n", encoding="utf-8")
    else:
        print(result)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

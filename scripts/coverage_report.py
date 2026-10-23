#!/usr/bin/env python3
import csv
import sys
from pathlib import Path

def print_coverage_for_module(module_name: str, csv_path: Path):
    if not csv_path.exists():
        print(f"[-] Coverage report for {module_name} not found at {csv_path}")
        return

    print("=" * 105)
    print(f" MODULE: {module_name.upper()} (JaCoCo Code Coverage)")
    print("=" * 105)
    print(f"{'Package / Class':<50} | {'Lines':<24} | {'Branches':<24}")
    print("-" * 105)

    tot_line_c = 0
    tot_line_m = 0
    tot_br_c = 0
    tot_br_m = 0

    with open(csv_path, mode="r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            cls_name = row["CLASS"]
            pkg_name = row["PACKAGE"]
            line_c = int(row["LINE_COVERED"])
            line_m = int(row["LINE_MISSED"])
            br_c = int(row["BRANCH_COVERED"])
            br_m = int(row["BRANCH_MISSED"])

            line_tot = line_c + line_m
            br_tot = br_c + br_m

            tot_line_c += line_c
            tot_line_m += line_m
            tot_br_c += br_c
            tot_br_m += br_m

            line_pct = (line_c / line_tot * 100) if line_tot > 0 else 100.0
            br_pct = (br_c / br_tot * 100) if br_tot > 0 else 100.0

            display_name = f"{pkg_name.split('.')[-1]}.{cls_name}"
            line_str = f"{line_c}/{line_tot} ({line_pct:5.1f}%)"
            br_str = f"{br_c}/{br_tot} ({br_pct:5.1f}%)" if br_tot > 0 else "N/A (no branches)"

            print(f"{display_name:<50} | {line_str:<24} | {br_str:<24}")

    print("-" * 105)
    overall_line_tot = tot_line_c + tot_line_m
    overall_br_tot = tot_br_c + tot_br_m
    overall_line_pct = (tot_line_c / overall_line_tot * 100) if overall_line_tot > 0 else 100.0
    overall_br_pct = (tot_br_c / overall_br_tot * 100) if overall_br_tot > 0 else 100.0

    tot_line_str = f"{tot_line_c}/{overall_line_tot} ({overall_line_pct:5.1f}%)"
    tot_br_str = f"{tot_br_c}/{overall_br_tot} ({overall_br_pct:5.1f}%)"
    print(f"{'OVERALL TOTAL':<50} | {tot_line_str:<24} | {tot_br_str:<24}")
    print("=" * 105)
    print()

def main():
    root = Path(__file__).resolve().parent.parent
    root_csv = root / "build" / "reports" / "jacoco" / "root" / "jacocoRootReport.csv"
    domain_csv = root / "domain" / "build" / "reports" / "jacoco" / "test" / "jacocoTestReport.csv"
    data_csv = root / "web" / "data" / "build" / "reports" / "jacoco" / "test" / "jacocoTestReport.csv"

    if "--modules" in sys.argv:
        print_coverage_for_module("domain", domain_csv)
        print_coverage_for_module("web:data", data_csv)
    elif root_csv.exists():
        print_coverage_for_module("WHOLE PROJECT AGGREGATE (:domain + :web:data)", root_csv)
    else:
        print_coverage_for_module("domain", domain_csv)
        print_coverage_for_module("web:data", data_csv)

if __name__ == "__main__":
    main()

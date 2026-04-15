import pdfplumber
import sys

# Set stdout to utf-8 to avoid encoding errors in the terminal
sys.stdout.reconfigure(encoding='utf-8')

for fname in ['下游']:
    print(f'\n{"="*60}')
    print(f'  {fname}.pdf')
    print(f'{"="*60}')
    try:
        with pdfplumber.open(rf'c:\Users\lulu97\Desktop\jyfq-api\doc\{fname}.pdf') as pdf:
            for i, page in enumerate(pdf.pages):
                print(f'--- Page {i+1} ---')
                t = page.extract_text()
                if t:
                    print(t)
                tables = page.extract_tables()
                if tables:
                    for tbl in tables:
                        for row in tbl:
                            print('\t| '.join(str(c).replace("\n", " ") if c else '' for c in row))
                        print()
    except Exception as e:
        print(f"Error reading {fname}: {e}")

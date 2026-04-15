import pdfplumber

for fname in ['上游', '下游']:
    print(f'\n{"="*60}')
    print(f'  {fname}.pdf')
    print(f'{"="*60}')
    with pdfplumber.open(rf'c:\Users\lulu97\Desktop\jyfq-api\doc\{fname}.pdf') as pdf:
        for i, page in enumerate(pdf.pages):
            print(f'--- Page {i+1} ---')
            t = page.extract_text()
            if t:
                print(t)
            # also try tables
            tables = page.extract_tables()
            for tbl in tables:
                for row in tbl:
                    print('\t| '.join(str(c) if c else '' for c in row))
                print()

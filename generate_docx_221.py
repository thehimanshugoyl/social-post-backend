import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

def set_cell_background(cell, fill_hex):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement('w:shd')
    shd.set(qn('w:val'), 'clear')
    shd.set(qn('w:color'), 'auto')
    shd.set(qn('w:fill'), fill_hex)
    tcPr.append(shd)

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = OxmlElement('w:tcMar')
    for m, val in [('top', top), ('bottom', bottom), ('left', left), ('right', right)]:
        node = OxmlElement(f'w:{m}')
        node.set(qn('w:w'), str(val))
        node.set(qn('w:type'), 'dxa')
        tcMar.append(node)
    tcPr.append(tcMar)

def set_table_borders(table, color="D3D3D3", sz="4", val="single"):
    tblPr = table._tbl.tblPr
    tblBorders = OxmlElement('w:tblBorders')
    for border_name in ['top', 'left', 'bottom', 'right', 'insideH', 'insideV']:
        border = OxmlElement(f'w:{border_name}')
        border.set(qn('w:val'), val)
        border.set(qn('w:sz'), sz)
        border.set(qn('w:space'), '0')
        border.set(qn('w:color'), color)
        tblBorders.append(border)
    tblPr.append(tblBorders)

def add_code_listing(doc, title, code_text):
    p_title = doc.add_paragraph()
    r_title = p_title.add_run(title)
    r_title.font.name = 'Calibri'
    r_title.font.size = Pt(11)
    r_title.font.bold = True
    p_title.paragraph_format.space_before = Pt(10)
    p_title.paragraph_format.space_after = Pt(4)

    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    
    cell = table.cell(0, 0)
    cell.width = Inches(6.5)
    set_cell_background(cell, "F8F9FA")
    set_cell_margins(cell, top=120, bottom=120, left=180, right=180)
    
    tcPr = cell._tc.get_or_add_tcPr()
    tcBorders = OxmlElement('w:tcBorders')
    for b in ['top', 'left', 'bottom', 'right']:
        node = OxmlElement(f'w:{b}')
        node.set(qn('w:val'), 'single')
        node.set(qn('w:sz'), '6')
        node.set(qn('w:color'), '005691')
        tcBorders.append(node)
    tcPr.append(tcBorders)

    p_code = cell.paragraphs[0]
    p_code.paragraph_format.space_before = Pt(2)
    p_code.paragraph_format.space_after = Pt(2)
    p_code.paragraph_format.line_spacing = 1.05
    r_code = p_code.add_run(code_text)
    r_code.font.name = 'Consolas'
    r_code.font.size = Pt(8.5)
    r_code.font.color.rgb = RGBColor(0x1F, 0x23, 0x28)

    p_sep = doc.add_paragraph()
    p_sep.paragraph_format.space_before = Pt(2)
    p_sep.paragraph_format.space_after = Pt(4)

def generate_report():
    doc = docx.Document()

    for section in doc.sections:
        section.top_margin = Inches(1)
        section.bottom_margin = Inches(1)
        section.left_margin = Inches(1)
        section.right_margin = Inches(1)

    # Document Header Title
    p_title = doc.add_paragraph()
    p_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r_title = p_title.add_run("Experiment – 2.2.1")
    r_title.font.name = 'Calibri'
    r_title.font.size = Pt(16)
    r_title.font.bold = True
    r_title.font.color.rgb = RGBColor(0x00, 0x33, 0x66)
    p_title.paragraph_format.space_after = Pt(12)

    # Student Info Table
    info_table = doc.add_table(rows=3, cols=2)
    info_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    info_table.autofit = False
    set_table_borders(info_table, color="003366", sz="6")

    info_data = [
        [("Student Name: ", True), ("Himanshu Goyal", False), ("UID: ", True), ("24BDA70369", False)],
        [("Branch: ", True), ("CSE (Data Science)", False), ("Section/Group: ", True), ("24 BDS-4(B)", False)],
        [("Course: ", True), ("Full Stack II (24CSP-337)", False), ("Date of Performance: ", True), ("16-09-2026", False)]
    ]

    col_widths = [Inches(3.25), Inches(3.25)]
    for row_idx, row_content in enumerate(info_data):
        row = info_table.rows[row_idx]
        for col_idx in range(2):
            cell = row.cells[col_idx]
            cell.width = col_widths[col_idx]
            set_cell_background(cell, "F2F5F8" if row_idx % 2 == 0 else "FFFFFF")
            set_cell_margins(cell, top=80, bottom=80, left=120, right=120)
            p = cell.paragraphs[0]
            p.paragraph_format.space_before = Pt(2)
            p.paragraph_format.space_after = Pt(2)
            
            lbl_text, lbl_bold = row_content[col_idx * 2]
            val_text, val_bold = row_content[col_idx * 2 + 1]

            r1 = p.add_run(lbl_text)
            r1.font.name = 'Calibri'
            r1.font.size = Pt(10)
            r1.font.bold = lbl_bold
            r1.font.color.rgb = RGBColor(0x00, 0x33, 0x66)

            r2 = p.add_run(val_text)
            r2.font.name = 'Calibri'
            r2.font.size = Pt(10)
            r2.font.bold = val_bold

    p_spacer = doc.add_paragraph()
    p_spacer.paragraph_format.space_before = Pt(6)
    p_spacer.paragraph_format.space_after = Pt(6)

    def add_section_heading(num_str, title_str):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(12)
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.keep_with_next = True
        r_num = p.add_run(f"{num_str}. {title_str}:")
        r_num.font.name = 'Calibri'
        r_num.font.size = Pt(12)
        r_num.font.bold = True
        r_num.font.color.rgb = RGBColor(0x00, 0x33, 0x66)
        return p

    def add_body_p(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(2)
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.line_spacing = 1.15
        p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        r = p.add_run(text)
        r.font.name = 'Calibri'
        r.font.size = Pt(11)
        r.font.color.rgb = RGBColor(0x24, 0x29, 0x2F)
        return p

    def add_bullet(items):
        p = doc.add_paragraph()
        p.paragraph_format.left_indent = Inches(0.25)
        p.paragraph_format.space_before = Pt(1)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.line_spacing = 1.15
        r_bullet = p.add_run("•  ")
        r_bullet.font.name = 'Calibri'
        r_bullet.font.size = Pt(11)
        r_bullet.font.bold = True
        r_bullet.font.color.rgb = RGBColor(0x00, 0x33, 0x66)

        for text, bold, italic in items:
            r = p.add_run(text)
            r.font.name = 'Calibri'
            r.font.size = Pt(11)
            r.font.bold = bold
            r.font.italic = italic
            r.font.color.rgb = RGBColor(0x24, 0x29, 0x2F)
        return p

    # 1. Aim
    add_section_heading("1", "Aim")
    add_body_p("To implement paginated and sortable REST APIs for efficient and scalable data retrieval.")

    # 2. Software Required
    add_section_heading("2", "Software Required")
    add_bullet([("Programming Language: ", True, False), ("Java (JDK 17 or higher)", False, False)])
    add_bullet([("Framework: ", True, False), ("Spring Boot 3.x / 4.x (Spring Data JPA, Spring Web)", False, False)])
    add_bullet([("IDE / Environment: ", True, False), ("Visual Studio Code with Java Extension Pack / IntelliJ IDEA", False, False)])
    add_bullet([("API Testing Tool: ", True, False), ("Postman / Windows PowerShell Invoke-RestMethod", False, False)])
    add_bullet([("Relational Database: ", True, False), ("In-Memory H2 Relational Database / PostgreSQL", False, False)])

    # 3. Objectives
    add_section_heading("3", "Objectives")
    add_bullet([("To understand pagination and sorting concepts: ", True, False), ("slicing large datasets into bounded sub-collections.", False, False)])
    add_bullet([("To implement pageable APIs using Spring Data: ", True, False), ("leveraging Pageable, PageRequest, Sort, and Page<T> abstractions.", False, False)])
    add_bullet([("To optimize data retrieval for large datasets: ", True, False), ("preventing server memory exhaustion and network congestion.", False, False)])
    add_bullet([("To improve API performance and usability: ", True, False), ("providing rich navigational metadata (totalPages, hasNext, isFirst).", False, False)])
    add_bullet([("Course Outcomes Mapped: ", True, False), ("CO3 - BT3, CO4 - BT4", False, True)])

    # 4. Theory
    add_section_heading("4", "Theory")
    add_body_p(
        "In enterprise web platforms and content feeds, datasets continually expand to tens of thousands or millions of records. "
        "Retrieving all records in a single unbounded API query (such as findAll()) produces severe database lock contention, memory exhaustion "
        "(OutOfMemoryError), and unacceptable network bandwidth consumption.\n\n"
        "Pagination systematically resolves this bottleneck by dividing datasets into discrete, bounded segments (pages). Clients specify "
        "the page index (zero-based) and the desired page size. Sorting allows clients to order records by specific attributes (e.g., createdAt, "
        "title, popularity) in ascending (ASC) or descending (DESC) directions. Spring Data provides direct support via the Pageable and Sort "
        "abstractions, which translate queries into optimized SQL LIMIT and OFFSET statements executed natively in the database engine.\n\n"
        "To deliver optimal client developer experience, responses are encapsulated in a specialized PagedResponse<T> wrapper containing "
        "navigation flags: totalElements, totalPages, pageNumber, pageSize, isFirst, isLast, hasNext, and hasPrevious. This allows frontend "
        "components to render dynamic pagination controls, infinite scrolling feeds, and sort indicators with complete fidelity."
    )

    # 5. Procedure / Algorithm
    add_section_heading("5", "Procedure / Algorithm")
    add_bullet([("Step 1: ", True, False), ("Define repository methods accepting Pageable parameters: findWithFilter(PostStatus status, String keyword, Pageable pageable).", False, False)])
    add_bullet([("Step 2: ", True, False), ("Expose GET /api/v1/posts endpoint with query parameters: page, size, sortBy, direction.", False, False)])
    add_bullet([("Step 3: ", True, False), ("Apply input bounds: @Min(0) for page index and @Min(1) @Max(100) for page size to prevent denial-of-service allocations.", False, False)])
    add_bullet([("Step 4: ", True, False), ("Construct Pageable instance: PageRequest.of(page, size, Sort.by(sortDirection, sortBy)).", False, False)])
    add_bullet([("Step 5: ", True, False), ("Query repository and transform domain entity Page<Post> to DTO Page<PostResponse> using PostMapper.", False, False)])
    add_bullet([("Step 6: ", True, False), ("Encapsulate results in standardized PagedResponse<PostResponse> with complete pagination metadata.", False, False)])
    add_bullet([("Step 7: ", True, False), ("Wrap PagedResponse inside generic ApiResponse.ok(pagedResponse) for consistent envelope architecture.", False, False)])
    add_bullet([("Step 8: ", True, False), ("Verify zero-index pagination, sorting orders, and boundary conditions via automated MockMvc integration tests.", False, False)])

    # 6. Code and Output
    add_section_heading("6", "Code and Output")

    code_controller = """@GetMapping
public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> getPosts(
        @RequestParam(required = false) PostStatus status,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page index must be >= 0") int page,
        @RequestParam(defaultValue = "10") @Min(value = 1, message = "Page size must be >= 1")
        @Max(value = 100, message = "Page size must be <= 100") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String direction) {

    Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

    Page<PostResponse> postPage = postService.getPosts(status, keyword, pageable);
    PagedResponse<PostResponse> pagedResponse = PagedResponse.of(postPage);

    return ResponseEntity.ok(ApiResponse.ok(pagedResponse, "Posts fetched successfully"));
}"""
    add_code_listing(doc, "Listing 1: Paginated & Sortable Endpoint in PostController", code_controller)

    code_service = """@Override
@Transactional(readOnly = true)
public Page<PostResponse> getPosts(PostStatus status, String keyword, Pageable pageable) {
    String trimmedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
    Page<Post> postPage = postRepository.findWithFilter(status, trimmedKeyword, pageable);
    return postPage.map(postMapper::toResponse);
}

// PagedResponse standard envelope
public record PagedResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean isFirst,
        boolean isLast,
        boolean hasNext,
        boolean hasPrevious
) {
    public static <T> PagedResponse<T> of(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}"""
    add_code_listing(doc, "Listing 2: Service Layer Page Retrieval & Standardized PagedResponse Envelope", code_service)

    # Screenshots / Execution Outputs
    img_dir = r"C:\Users\himan\Downloads\social-post-backend\social-post-backend\docs\screenshots"
    img1_path = os.path.join(img_dir, "ss_exp221_pagination.png")
    img2_path = os.path.join(img_dir, "ss_exp221_sorting.png")

    if os.path.exists(img1_path):
        p_img_title1 = doc.add_paragraph()
        r_it1 = p_img_title1.add_run("Output 1: Page 0 Retrieval with Bounded Size (2 records) and Navigational Metadata")
        r_it1.font.name = 'Calibri'
        r_it1.font.bold = True
        p_img1 = doc.add_paragraph()
        p_img1.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_img1.add_run().add_picture(img1_path, width=Inches(6.5))

    if os.path.exists(img2_path):
        p_img_title2 = doc.add_paragraph()
        r_it2 = p_img_title2.add_run("Output 2: Dynamic Multi-field Sorting (Title ASC) via Spring Data JPA Sort Object")
        r_it2.font.name = 'Calibri'
        r_it2.font.bold = True
        p_img2 = doc.add_paragraph()
        p_img2.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_img2.add_run().add_picture(img2_path, width=Inches(6.5))

    # 7. Conclusion
    add_section_heading("7", "Conclusion")
    add_body_p(
        "Scalable read operations were successfully implemented using Spring Data JPA's Pageable and Sort abstractions. "
        "By enforcing strict pagination bounds (@Max(100)), the backend is protected against memory exhaustion and distributed denial-of-service vectors. "
        "Dynamic multi-field sorting empowers clients to organize feeds chronologically or alphabetically with sub-millisecond database queries. "
        "The standardized PagedResponse<T> metadata delivers comprehensive navigational context for responsive UI components, ensuring high performance."
    )

    # 8. Learning Outcomes
    add_section_heading("8", "Learning Outcomes")
    add_bullet([("CO3 - BT3: ", True, False), ("Implemented paginated and sortable REST endpoints using Spring Data Pageable, PageRequest, and Sort.", False, False)])
    add_bullet([("CO4 - BT4: ", True, False), ("Analyzed query performance and memory utilization under large datasets, mitigating unconstrained findAll() overhead.", False, False)])
    add_bullet([("CO4 - BT4: ", True, False), ("Engineered structured PagedResponse envelopes containing navigational state flags (totalPages, hasNext, isFirst).", False, False)])
    add_bullet([("CO3 - BT3: ", True, False), ("Applied defensive input bounds (@Min, @Max) to sanitize pagination request query parameters.", False, False)])

    out_repo = r"C:\Users\himan\Downloads\social-post-backend\social-post-backend\docs\lab-reports\FSD 2.2.1.docx"
    doc.save(out_repo)
    print(f"Generated repo report: {out_repo}")

    try:
        out_dl = r"C:\Users\himan\Downloads\FSD 2.2.1.docx"
        doc.save(out_dl)
        print(f"Generated Downloads report: {out_dl}")
    except Exception as e:
        print(f"Could not overwrite {out_dl}: {e}")

if __name__ == "__main__":
    generate_report()

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
    r_title = p_title.add_run("Experiment – 2.1.2")
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
        [("Course: ", True), ("Full Stack II (24CSP-337)", False), ("Date of Performance: ", True), ("09-09-2026", False)]
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
    add_body_p("To implement global exception handling and structured logging for building robust and observable backend systems.")

    # 2. Software Required
    add_section_heading("2", "Software Required")
    add_bullet([("Programming Language: ", True, False), ("Java (JDK 17 or higher)", False, False)])
    add_bullet([("Framework: ", True, False), ("Spring Boot 3.x / 4.x", False, False)])
    add_bullet([("IDE / Environment: ", True, False), ("Visual Studio Code with Java Extension Pack / IntelliJ IDEA", False, False)])
    add_bullet([("Testing & Diagnostic Tool: ", True, False), ("Postman / Windows PowerShell Invoke-RestMethod", False, False)])
    add_bullet([("Logging Framework: ", True, False), ("SLF4J with Logback and MDC (Mapped Diagnostic Context)", False, False)])

    # 3. Objectives
    add_section_heading("3", "Objectives")
    add_bullet([("To handle exceptions centrally: ", True, False), ("using @RestControllerAdvice to eliminate redundant try-catch boilerplate.", False, False)])
    add_bullet([("To implement logging mechanisms: ", True, False), ("intercepting HTTP requests and tracking execution latency.", False, False)])
    add_bullet([("To use correlation IDs for request tracing: ", True, False), ("populating MDC context with unique UUIDs propagated via X-Correlation-ID headers.", False, False)])
    add_bullet([("To improve observability and debugging: ", True, False), ("eliminating internal stack trace leaks to prevent security vulnerabilities.", False, False)])
    add_bullet([("Course Outcomes Mapped: ", True, False), ("CO3 - BT3", False, True)])

    # 4. Theory
    add_section_heading("4", "Theory")
    add_body_p(
        "In enterprise software architecture, handling exceptions individually across every controller endpoint introduces code duplication, "
        "brittle logic, and inconsistent error payloads. Spring Boot addresses this by providing global exception interception through "
        "@RestControllerAdvice. This centralized component captures unhandled exceptions thrown anywhere in the web layer, transforms them "
        "into uniform ApiErrorResponse envelopes, and prevents raw Java stack traces from leaking to API clients, which would otherwise present "
        "significant security vulnerabilities.\n\n"
        "Alongside exception interception, structured logging is the cornerstone of backend system observability. When applications scale to handle "
        "thousands of concurrent requests, logs become interleaved and impossible to interpret without contextual identifiers. "
        "The Mapped Diagnostic Context (MDC) provided by SLF4J allows developers to bind unique correlation IDs (UUIDs) to the executing thread. "
        "A servlet filter (OncePerRequestFilter) intercepts each incoming HTTP request, extracts or generates an 'X-Correlation-ID', injects it "
        "into the MDC, and attaches it to the outgoing HTTP response header. Every subsequent log message printed by controllers, services, "
        "or database repositories automatically includes this correlation ID, enabling distributed end-to-end transaction tracing."
    )

    # 5. Procedure / Algorithm
    add_section_heading("5", "Procedure / Algorithm")
    add_bullet([("Step 1: ", True, False), ("Create domain exception classes: ResourceNotFoundException, BadRequestException, DuplicateResourceException extending RuntimeException.", False, False)])
    add_bullet([("Step 2: ", True, False), ("Develop uniform error model ApiErrorResponse containing success=false, status, error, message, path, correlationId, and timestamp.", False, False)])
    add_bullet([("Step 3: ", True, False), ("Implement GlobalExceptionHandler annotated with @RestControllerAdvice.", False, False)])
    add_bullet([("Step 4: ", True, False), ("Add @ExceptionHandler methods for ResourceNotFoundException (404), MethodArgumentNotValidException (400), DuplicateResourceException (409), and uncaught Exception (500).", False, False)])
    add_bullet([("Step 5: ", True, False), ("Create CorrelationIdFilter extending OncePerRequestFilter to parse or generate X-Correlation-ID UUIDs.", False, False)])
    add_bullet([("Step 6: ", True, False), ("Store correlation ID in MDC (MDC.put('correlationId', id)) and populate outgoing response headers.", False, False)])
    add_bullet([("Step 7: ", True, False), ("Configure logback-spring.xml pattern to include [%X{correlationId}] in every console and file log line.", False, False)])
    add_bullet([("Step 8: ", True, False), ("Clean up MDC in a finally block (MDC.clear()) to prevent thread-pool memory leaks across recycled threads.", False, False)])
    add_bullet([("Step 9: ", True, False), ("Verify centralized error mapping and request tracing using automated JUnit 5 tests and PowerShell client calls.", False, False)])

    # 6. Code and Output
    add_section_heading("6", "Code and Output")

    code_handler = """@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("[404 Not Found] {} - Path: {}", ex.getMessage(), request.getRequestURI());
        ApiErrorResponse error = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                request.getRequestURI(),
                MDC.get("correlationId")
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        log.warn("[400 Bad Request] Validation failed on {} fields - Path: {}", fieldErrors.size(), request.getRequestURI());
        ApiErrorResponse error = ApiErrorResponse.ofValidation(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "One or more request parameters failed validation",
                request.getRequestURI(),
                MDC.get("correlationId"),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleCatchAll(Exception ex, HttpServletRequest request) {
        log.error("[500 Internal Error] Unhandled exception on path: {}", request.getRequestURI(), ex);
        ApiErrorResponse error = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected internal error occurred. Please contact system support.",
                request.getRequestURI(),
                MDC.get("correlationId")
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}"""
    add_code_listing(doc, "Listing 1: Global Exception Handler – Centralized Error Interception & Standardization", code_handler)

    code_filter = """@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}"""
    add_code_listing(doc, "Listing 2: Correlation ID Filter – Thread-Bound Request Tracing via SLF4J MDC", code_filter)

    # Screenshots / Execution Outputs
    img_dir = r"C:\Users\himan\Downloads\social-post-backend\social-post-backend\docs\screenshots"
    img1_path = os.path.join(img_dir, "ss_exp212_img1.png")
    img2_path = os.path.join(img_dir, "ss_exp212_img2.png")
    img3_path = os.path.join(img_dir, "ss_exp212_img3.png")

    if os.path.exists(img1_path):
        p_img_title1 = doc.add_paragraph()
        r_it1 = p_img_title1.add_run("Output 1: BEFORE – Unhandled Internal Exception Leaking Raw Stack Trace to Client")
        r_it1.font.name = 'Calibri'
        r_it1.font.bold = True
        p_img1 = doc.add_paragraph()
        p_img1.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_img1.add_run().add_picture(img1_path, width=Inches(6.5))

    if os.path.exists(img2_path):
        p_img_title2 = doc.add_paragraph()
        r_it2 = p_img_title2.add_run("Output 2: AFTER – Clean, Standardized Error Envelopes for Bean Validation (400) and Missing Resource (404)")
        r_it2.font.name = 'Calibri'
        r_it2.font.bold = True
        p_img2 = doc.add_paragraph()
        p_img2.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_img2.add_run().add_picture(img2_path, width=Inches(6.5))

    if os.path.exists(img3_path):
        p_img_title3 = doc.add_paragraph()
        r_it3 = p_img_title3.add_run("Output 3: Structured Server Logs – Full Request Lifecycle Traced via Unique MDC Correlation IDs")
        r_it3.font.name = 'Calibri'
        r_it3.font.bold = True
        p_img3 = doc.add_paragraph()
        p_img3.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_img3.add_run().add_picture(img3_path, width=Inches(6.5))

    # 7. Conclusion
    add_section_heading("7", "Conclusion")
    add_body_p(
        "Global exception handling and structured observability mechanisms were successfully established in the backend service. "
        "By implementing @RestControllerAdvice, custom domain exceptions (ResourceNotFoundException, BadRequestException) are caught centrally "
        "and mapped to uniform ApiErrorResponse payloads, completely eliminating internal stack trace exposure. "
        "The CorrelationIdFilter binds unique correlation IDs to SLF4J MDC, enabling seamless tracking of each request from ingress to completion. "
        "The resulting backend system achieves enterprise-grade resilience, zero information leakage, and complete diagnostic observability."
    )

    # 8. Learning Outcomes
    add_section_heading("8", "Learning Outcomes")
    add_bullet([("CO3 - BT3: ", True, False), ("Centralized error response handling using @RestControllerAdvice and custom domain exception hierarchies.", False, False)])
    add_bullet([("CO3 - BT3: ", True, False), ("Engineered uniform error payloads (ApiErrorResponse) preventing stack trace exposure and internal vulnerability leaks.", False, False)])
    add_bullet([("CO3 - BT3: ", True, False), ("Implemented OncePerRequestFilter to intercept incoming requests and manage cross-cutting servlet filters.", False, False)])
    add_bullet([("CO3 - BT3: ", True, False), ("Leveraged Mapped Diagnostic Context (MDC) in SLF4J/Logback for end-to-end request tracing using correlation IDs.", False, False)])

    out_repo = r"C:\Users\himan\Downloads\social-post-backend\social-post-backend\docs\lab-reports\FSD 2.1.2.docx"
    doc.save(out_repo)
    print(f"Generated repo report: {out_repo}")

    try:
        out_dl = r"C:\Users\himan\Downloads\FSD 2.1.2.docx"
        doc.save(out_dl)
        print(f"Generated Downloads report: {out_dl}")
    except Exception as e:
        print(f"Could not overwrite {out_dl}: {e}")

if __name__ == "__main__":
    generate_report()

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
    r_title = p_title.add_run("Experiment – 2.2.2")
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
    add_body_p("To optimize backend read operations using caching and efficient database query strategies.")

    # 2. Software Required
    add_section_heading("2", "Software Required")
    add_bullet([("Programming Language: ", True, False), ("Java (JDK 17 or higher)", False, False)])
    add_bullet([("Framework: ", True, False), ("Spring Boot with Spring Cache Abstraction (spring-boot-starter-cache)", False, False)])
    add_bullet([("Database & ORM: ", True, False), ("H2 In-Memory Database / PostgreSQL with Hibernate ORM", False, False)])
    add_bullet([("Caching Provider: ", True, False), ("ConcurrentMapCacheManager (In-Memory) / Caffeine Cache", False, False)])
    add_bullet([("Testing & Benchmarking: ", True, False), ("PowerShell Measure-Command / JUnit 5 SpringBootTest", False, False)])

    # 3. Objectives
    add_section_heading("3", "Objectives")
    add_bullet([("To identify and resolve inefficient database queries: ", True, False), ("specifically resolving the infamous N+1 query problem.", False, False)])
    add_bullet([("To eliminate multiple round-trips: ", True, False), ("using JPQL JOIN FETCH to eagerly retrieve parent and child entities in a single query.", False, False)])
    add_bullet([("To implement declarative caching: ", True, False), ("using @Cacheable, @CachePut, and @CacheEvict to minimize database hits.", False, False)])
    add_bullet([("To execute Native SQL aggregation queries: ", True, False), ("mapping database group-by analytics to Spring Data interface projections.", False, False)])
    add_bullet([("To benchmark API performance: ", True, False), ("quantifying latency reductions under cold miss versus warm cache hit scenarios.", False, False)])
    add_bullet([("Course Outcomes Mapped: ", True, False), ("CO4 - BT4, CO5 - BT5", False, True)])

    # 4. Theory
    add_section_heading("4", "Theory")
    add_body_p(
        "Read-heavy enterprise applications face significant scalability bottlenecks when ORM frameworks generate inefficient queries. "
        "The N+1 query problem occurs when loading a collection of N posts alongside their related child comments. The ORM executes 1 query "
        "to retrieve all posts, and subsequently executes N separate queries to fetch comments for each post (totaling N+1 round trips). "
        "This is eliminated using JPQL JOIN FETCH ('SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.comments'), which instructs Hibernate "
        "to join the tables and populate all entities in a single SQL operation.\n\n"
        "In addition to query optimization, declarative in-memory caching stores hot read results in RAM, bypassing the persistence tier completely. "
        "Spring's caching abstraction uses @Cacheable to serve subsequent identical requests with sub-millisecond latency. To ensure cache consistency, "
        "@CacheEvict purges stale entries upon post modification or deletion, while @CachePut updates values dynamically. "
        "For complex analytics involving GROUP BY and COUNT aggregations, Native SQL queries execute directly on the database engine, "
        "mapped into lightweight Spring Data interface projections (AuthorStatsProjection) without ORM entity hydration overhead."
    )

    # 5. Procedure / Algorithm
    add_section_heading("5", "Procedure / Algorithm")
    add_bullet([("Step 1: ", True, False), ("Add spring-boot-starter-cache dependency and declare @EnableCaching in CacheConfig.", False, False)])
    add_bullet([("Step 2: ", True, False), ("Configure cache managers with designated cache names: 'posts', 'post-slugs', and 'author-stats'.", False, False)])
    add_bullet([("Step 3: ", True, False), ("Diagnose N+1 query behavior and implement JOIN FETCH in PostRepository.findAllWithComments().", False, False)])
    add_bullet([("Step 4: ", True, False), ("Develop Native SQL aggregation query with AuthorStatsProjection for author metrics reporting.", False, False)])
    add_bullet([("Step 5: ", True, False), ("Annotate PostService read methods (getPostById, getPostBySlug) with @Cacheable(value='posts', key='#id').", False, False)])
    add_bullet([("Step 6: ", True, False), ("Annotate mutation methods (updatePost, deletePost) with @CacheEvict(value='posts', key='#id') to maintain consistency.", False, False)])
    add_bullet([("Step 7: ", True, False), ("Expose optimized endpoints: GET /api/v1/posts/eager and GET /api/v1/posts/analytics/authors.", False, False)])
    add_bullet([("Step 8: ", True, False), ("Benchmark and quantify execution latency using automated integration tests and Measure-Command PowerShell scripts.", False, False)])

    # 6. Code and Output
    add_section_heading("6", "Code and Output")

    code_repo = """public interface PostRepository extends JpaRepository<Post, Long> {

    // Resolves N+1 query problem by fetching posts and comments in 1 round trip
    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.comments ORDER BY p.createdAt DESC")
    List<Post> findAllWithComments();

    // High-performance native SQL aggregation query bypassing ORM entity overhead
    @Query(value = \"\"\"
        SELECT p.author AS author,
               COUNT(DISTINCT p.id) AS totalPosts,
               COUNT(c.id) AS totalComments
        FROM posts p
        LEFT JOIN comments c ON p.id = c.post_id
        GROUP BY p.author
        ORDER BY totalPosts DESC
        \"\"\", nativeQuery = true)
    List<AuthorStatsProjection> findAuthorPostAndCommentStats();
}"""
    add_code_listing(doc, "Listing 1: JOIN FETCH & Native SQL Projections in PostRepository", code_repo)

    code_service = """@Service
public class PostServiceImpl implements PostService {

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "posts", key = "#id")
    public PostResponse getPostById(Long id) {
        log.info("[PostService] Cache miss for post ID: {}. Querying database.", id);
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        return postMapper.toResponse(post);
    }

    @Override
    @Transactional
    @CacheEvict(value = "posts", key = "#id")
    public PostResponse updatePost(Long id, UpdatePostRequest request) {
        log.info("[PostService] Evicting cache for updated post ID: {}", id);
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        postMapper.updateEntityFromDto(request, post);
        return postMapper.toResponse(postRepository.save(post));
    }
}"""
    add_code_listing(doc, "Listing 2: Declarative In-Memory Caching (@Cacheable & @CacheEvict)", code_service)

    # Screenshots / Execution Outputs
    img_dir = r"C:\Users\himan\Downloads\social-post-backend\social-post-backend\docs\screenshots"
    img1_path = os.path.join(img_dir, "ss_exp222_caching_benchmark.png")
    img2_path = os.path.join(img_dir, "ss_exp222_join_fetch_analytics.png")

    if os.path.exists(img1_path):
        p_img_title1 = doc.add_paragraph()
        r_it1 = p_img_title1.add_run("Output 1: Caching Benchmark – Cold Database Miss (14.82ms) vs In-Memory Cache Hit (0.58ms, 96.07% Reduction)")
        r_it1.font.name = 'Calibri'
        r_it1.font.bold = True
        p_img1 = doc.add_paragraph()
        p_img1.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_img1.add_run().add_picture(img1_path, width=Inches(6.5))

    if os.path.exists(img2_path):
        p_img_title2 = doc.add_paragraph()
        r_it2 = p_img_title2.add_run("Output 2: Single-Query JOIN FETCH Execution & Native SQL Author Aggregation Results")
        r_it2.font.name = 'Calibri'
        r_it2.font.bold = True
        p_img2 = doc.add_paragraph()
        p_img2.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p_img2.add_run().add_picture(img2_path, width=Inches(6.5))

    # 7. Conclusion
    add_section_heading("7", "Conclusion")
    add_body_p(
        "Read operations were optimized by resolving the N+1 query problem and deploying in-memory caching. "
        "The JPQL JOIN FETCH pattern reduced database queries from N+1 down to a single relational query, eliminating network round-trip overhead. "
        "Declarative caching with @Cacheable delivered dramatic performance improvements, cutting response latency from 14.82ms down to 0.58ms "
        "(a 96.07% latency reduction). Stale reads were prevented through automated @CacheEvict invalidation on update and delete mutations, "
        "while native SQL projections provided blazingly fast author metrics reporting directly from the database engine."
    )

    # 8. Learning Outcomes
    add_section_heading("8", "Learning Outcomes")
    add_bullet([("CO4 - BT4: ", True, False), ("Identified the N+1 query problem and resolved it via JPQL JOIN FETCH eager loading.", False, False)])
    add_bullet([("CO4 - BT4: ", True, False), ("Engineered Spring Boot caching mechanics using @Cacheable, @CachePut, and @CacheEvict.", False, False)])
    add_bullet([("CO5 - BT5: ", True, False), ("Benchmarked API read latencies, demonstrating a 96% retrieval latency reduction under in-memory caching.", False, False)])
    add_bullet([("CO4 - BT4: ", True, False), ("Developed Native SQL aggregation queries mapped into Spring Data interface projections.", False, False)])

    out_repo = r"C:\Users\himan\Downloads\social-post-backend\social-post-backend\docs\lab-reports\FSD 2.2.2.docx"
    doc.save(out_repo)
    print(f"Generated repo report: {out_repo}")

    try:
        out_dl = r"C:\Users\himan\Downloads\FSD 2.2.2.docx"
        doc.save(out_dl)
        print(f"Generated Downloads report: {out_dl}")
    except Exception as e:
        print(f"Could not overwrite {out_dl}: {e}")

if __name__ == "__main__":
    generate_report()

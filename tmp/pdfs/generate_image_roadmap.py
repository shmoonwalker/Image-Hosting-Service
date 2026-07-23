from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfbase import pdfmetrics
from reportlab.platypus import (
    BaseDocTemplate,
    Flowable,
    Frame,
    KeepTogether,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
)


OUTPUT = "output/pdf/image-hosting-service-roadmap-checklist.pdf"
PAGE_W, PAGE_H = A4

NAVY = colors.HexColor("#102A43")
BLUE = colors.HexColor("#1769E0")
LIGHT_BLUE = colors.HexColor("#EAF2FF")
TEAL = colors.HexColor("#0B8F7A")
LIGHT_TEAL = colors.HexColor("#E8F7F3")
AMBER = colors.HexColor("#D97706")
LIGHT_AMBER = colors.HexColor("#FFF5E5")
INK = colors.HexColor("#243B53")
MUTED = colors.HexColor("#627D98")
LINE = colors.HexColor("#D9E2EC")
PAPER = colors.HexColor("#F7F9FC")
WHITE = colors.white


styles = getSampleStyleSheet()
styles.add(ParagraphStyle(
    name="CoverTitle",
    parent=styles["Title"],
    fontName="Helvetica-Bold",
    fontSize=27,
    leading=32,
    textColor=WHITE,
    alignment=TA_LEFT,
    spaceAfter=8,
))
styles.add(ParagraphStyle(
    name="CoverSubtitle",
    parent=styles["Normal"],
    fontName="Helvetica",
    fontSize=12,
    leading=17,
    textColor=colors.HexColor("#D9EAF7"),
))
styles.add(ParagraphStyle(
    name="SectionTitle",
    parent=styles["Heading1"],
    fontName="Helvetica-Bold",
    fontSize=18,
    leading=22,
    textColor=NAVY,
    spaceAfter=8,
))
styles.add(ParagraphStyle(
    name="SectionIntro",
    parent=styles["Normal"],
    fontName="Helvetica",
    fontSize=9.5,
    leading=14,
    textColor=MUTED,
    spaceAfter=10,
))
styles.add(ParagraphStyle(
    name="GroupTitle",
    parent=styles["Heading2"],
    fontName="Helvetica-Bold",
    fontSize=11.5,
    leading=14,
    textColor=NAVY,
    spaceBefore=4,
    spaceAfter=5,
))
styles.add(ParagraphStyle(
    name="CheckText",
    parent=styles["Normal"],
    fontName="Helvetica",
    fontSize=9.2,
    leading=12.2,
    textColor=INK,
))
styles.add(ParagraphStyle(
    name="Small",
    parent=styles["Normal"],
    fontName="Helvetica",
    fontSize=8.2,
    leading=11,
    textColor=MUTED,
))
styles.add(ParagraphStyle(
    name="CalloutTitle",
    parent=styles["Heading3"],
    fontName="Helvetica-Bold",
    fontSize=11,
    leading=14,
    textColor=AMBER,
    spaceAfter=4,
))
styles.add(ParagraphStyle(
    name="CalloutBody",
    parent=styles["Normal"],
    fontName="Helvetica",
    fontSize=9.2,
    leading=13,
    textColor=INK,
))
styles.add(ParagraphStyle(
    name="RoadmapPhase",
    parent=styles["Normal"],
    fontName="Helvetica-Bold",
    fontSize=9.2,
    leading=11,
    alignment=TA_CENTER,
    textColor=NAVY,
))


class CheckBox(Flowable):
    def __init__(self, checked=False, status="planned"):
        super().__init__()
        self.checked = checked
        self.status = status
        self.width = 12
        self.height = 12

    def draw(self):
        c = self.canv
        if self.checked:
            c.setFillColor(TEAL)
            c.setStrokeColor(TEAL)
            c.roundRect(0, 0, 10, 10, 2, fill=1, stroke=1)
            c.setStrokeColor(WHITE)
            c.setLineWidth(1.4)
            c.line(2.2, 5.1, 4.3, 2.9)
            c.line(4.3, 2.9, 8.1, 7.6)
        elif self.status == "next":
            c.setFillColor(LIGHT_AMBER)
            c.setStrokeColor(AMBER)
            c.setLineWidth(1.2)
            c.roundRect(0, 0, 10, 10, 2, fill=1, stroke=1)
        else:
            c.setFillColor(WHITE)
            c.setStrokeColor(colors.HexColor("#9FB3C8"))
            c.setLineWidth(1)
            c.roundRect(0, 0, 10, 10, 2, fill=1, stroke=1)


def checklist(items, checked=False, next_first=False):
    rows = []
    for index, item in enumerate(items):
        status = "next" if next_first and index == 0 else "planned"
        rows.append([
            CheckBox(checked=checked, status=status),
            Paragraph(item, styles["CheckText"]),
        ])
    table = Table(rows, colWidths=[15, 160 * mm], hAlign="LEFT")
    table.setStyle(TableStyle([
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("TOPPADDING", (0, 0), (-1, -1), 2.2),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 2.2),
        ("LEFTPADDING", (0, 0), (-1, -1), 0),
        ("RIGHTPADDING", (0, 0), (-1, -1), 4),
    ]))
    return table


def section(title, intro, groups):
    content = [Paragraph(title, styles["SectionTitle"])]
    if intro:
        content.append(Paragraph(intro, styles["SectionIntro"]))
    for group_title, items, checked, next_first in groups:
        block = [
            Paragraph(group_title, styles["GroupTitle"]),
            checklist(items, checked=checked, next_first=next_first),
            Spacer(1, 4),
        ]
        content.append(KeepTogether(block))
    return content


def status_pill(text, fill, text_color):
    t = Table([[Paragraph(text, ParagraphStyle(
        name=f"Pill-{text}",
        parent=styles["Small"],
        fontName="Helvetica-Bold",
        fontSize=8,
        leading=10,
        alignment=TA_CENTER,
        textColor=text_color,
    ))]], colWidths=[37 * mm], rowHeights=[8 * mm])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), fill),
        ("BOX", (0, 0), (-1, -1), 0.7, text_color),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("LEFTPADDING", (0, 0), (-1, -1), 4),
        ("RIGHTPADDING", (0, 0), (-1, -1), 4),
    ]))
    return t


def draw_page(canvas, doc):
    canvas.saveState()
    page_num = canvas.getPageNumber()
    canvas.setFont("Helvetica", 7.8)
    canvas.setFillColor(MUTED)
    canvas.drawRightString(PAGE_W - 18 * mm, 8.5 * mm, f"Page {page_num}")
    canvas.restoreState()


doc = BaseDocTemplate(
    OUTPUT,
    pagesize=A4,
    leftMargin=18 * mm,
    rightMargin=18 * mm,
    topMargin=22 * mm,
    bottomMargin=18 * mm,
    title="Image Hosting Service Roadmap and Checklist",
    author="Project planning artifact",
)

frame = Frame(
    doc.leftMargin,
    doc.bottomMargin,
    doc.width,
    doc.height,
    leftPadding=0,
    rightPadding=0,
    topPadding=0,
    bottomPadding=0,
)
doc.addPageTemplates([PageTemplate(id="main", frames=[frame], onPageEnd=draw_page)])

story = []

# Cover and roadmap.
cover = Table([
    [Paragraph("Image Hosting Service", styles["CoverTitle"])],
    [Paragraph("Delivery roadmap and progress checklist", styles["CoverSubtitle"])],
    [Spacer(1, 8)],
    [Paragraph("Current checkpoint: authenticated upload and owner-only content retrieval are working end to end.", styles["CoverSubtitle"])],
], colWidths=[doc.width])
cover.setStyle(TableStyle([
    ("BACKGROUND", (0, 0), (-1, -1), NAVY),
    ("LEFTPADDING", (0, 0), (-1, -1), 14 * mm),
    ("RIGHTPADDING", (0, 0), (-1, -1), 14 * mm),
    ("TOPPADDING", (0, 0), (-1, 0), 15 * mm),
    ("BOTTOMPADDING", (0, -1), (-1, -1), 14 * mm),
]))
story.extend([cover, Spacer(1, 10 * mm)])

story.append(Paragraph("Progress at a glance", styles["SectionTitle"]))
pill_row = Table([[
    status_pill("COMPLETED", LIGHT_TEAL, TEAL),
    status_pill("NEXT", LIGHT_AMBER, AMBER),
    status_pill("PLANNED", PAPER, MUTED),
]], colWidths=[43 * mm, 43 * mm, 43 * mm], hAlign="LEFT")
pill_row.setStyle(TableStyle([
    ("LEFTPADDING", (0, 0), (-1, -1), 0),
    ("RIGHTPADDING", (0, 0), (-1, -1), 6),
]))
story.extend([pill_row, Spacer(1, 8 * mm)])

phases = [
    ("1. Upload + content", LIGHT_TEAL, TEAL),
    ("2. Owner library", LIGHT_AMBER, AMBER),
    ("3. Delete + visibility", PAPER, LINE),
    ("4. Async processing", PAPER, LINE),
    ("5. AI tagging", PAPER, LINE),
    ("6. Thumbnails", PAPER, LINE),
    ("7. Public gallery + search", PAPER, LINE),
    ("8. Sharing + hardening", PAPER, LINE),
    ("9. Cloud + frontend", PAPER, LINE),
]
phase_rows = []
for i in range(0, len(phases), 3):
    row = []
    for text, fill, border in phases[i:i + 3]:
        row.append(Paragraph(text, styles["RoadmapPhase"]))
    while len(row) < 3:
        row.append("")
    phase_rows.append(row)

roadmap = Table(phase_rows, colWidths=[55 * mm, 55 * mm, 55 * mm], rowHeights=[18 * mm] * 3)
roadmap_style = [
    ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
    ("ALIGN", (0, 0), (-1, -1), "CENTER"),
    ("LEFTPADDING", (0, 0), (-1, -1), 5),
    ("RIGHTPADDING", (0, 0), (-1, -1), 5),
]
for idx, (_, fill, border) in enumerate(phases):
    r, c = divmod(idx, 3)
    roadmap_style.extend([
        ("BACKGROUND", (c, r), (c, r), fill),
        ("BOX", (c, r), (c, r), 0.8, border),
    ])
roadmap.setStyle(TableStyle(roadmap_style))
story.extend([roadmap, Spacer(1, 8 * mm)])

next_box = Table([[
    Paragraph("RESUME HERE", styles["CalloutTitle"]),
    Paragraph("Finish <b>ImageRepository.countByOwnerId()</b>, create reusable <b>PageResponse&lt;T&gt;</b>, and expose <b>GET /api/v1/images/mine</b>.", styles["CalloutBody"]),
]], colWidths=[35 * mm, 125 * mm])
next_box.setStyle(TableStyle([
    ("BACKGROUND", (0, 0), (-1, -1), LIGHT_AMBER),
    ("BOX", (0, 0), (-1, -1), 0.8, AMBER),
    ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
    ("LEFTPADDING", (0, 0), (-1, -1), 8),
    ("RIGHTPADDING", (0, 0), (-1, -1), 8),
    ("TOPPADDING", (0, 0), (-1, -1), 8),
    ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
]))
story.append(next_box)
story.append(PageBreak())

# Completed slice and owner management.
story.extend(section(
    "1. Working foundation",
    "These capabilities have been implemented and manually verified with Postman. Automated coverage is still required before merge.",
    [
        ("Authenticated upload", [
            "Accept authenticated multipart uploads at <b>POST /api/v1/images</b>.",
            "Accept JPEG and PNG files up to 10 MB.",
            "Sanitize filenames and reject empty or oversized uploads.",
            "Decode actual image content and record width and height.",
            "Upload repeatable request bytes to private S3-compatible storage.",
            "Store object key and metadata in PostgreSQL.",
        ], True, False),
        ("Private content retrieval", [
            "Find image metadata by primary key.",
            "Allow the owner to retrieve a private image.",
            "Return the same 404 response for missing and inaccessible private images.",
            "Stream private object content through the backend.",
            "Set Content-Type, Content-Length, inline disposition, and nosniff headers.",
            "Manually verify a newly uploaded image renders through the content endpoint.",
        ], True, False),
        ("Deferred stabilization", [
            "Add validator unit tests.",
            "Add upload service and controller tests.",
            "Add owner, non-owner, missing-image, and streaming tests.",
        ], False, False),
    ],
))
story.append(PageBreak())

story.extend(section(
    "2. Owner library and image management",
    "Build the authenticated user's library before exposing public behavior. Each endpoint must enforce ownership and return predictable errors.",
    [
        ("Owned-image listing - next", [
            "Add <b>countByOwnerId()</b> for pagination metadata.",
            "Create reusable <b>PageResponse&lt;T&gt;</b>.",
            "Implement service pagination with page and size validation.",
            "Expose <b>GET /api/v1/images/mine?page=0&amp;size=20</b>.",
            "Return public and private images newest first.",
            "Add stable ordering and owner-listing tests.",
        ], False, True),
        ("Image metadata", [
            "Expose image metadata by ID.",
            "Allow public metadata later and owner-only private metadata.",
            "Avoid exposing storage keys or provider details.",
        ], False, False),
        ("Owner-only deletion", [
            "Expose <b>DELETE /api/v1/images/{imageId}</b>.",
            "Verify ownership before deleting anything.",
            "Delete the original object and thumbnail when present.",
            "Delete the database record safely.",
            "Define recovery behavior for partial storage/database failures.",
            "Prevent and clean up orphaned storage objects.",
        ], False, False),
        ("Database performance", [
            "Add a Flyway migration for the final owner-listing index.",
            "Align index ordering with owner_id, created_at DESC, and id DESC.",
            "Use EXPLAIN ANALYZE after representative data exists.",
        ], False, False),
    ],
))
story.append(PageBreak())

story.extend(section(
    "3. Visibility, processing, and thumbnails",
    "Make visibility explicit, then add asynchronous processing without slowing the upload response.",
    [
        ("Public and private visibility", [
            "Accept an explicit visibility choice during upload.",
            "Expose an owner-only visibility update endpoint.",
            "Allow anonymous access to public content and metadata.",
            "Keep private content available only to its owner or a valid share link.",
            "Exclude private images from galleries and search.",
            "Return indistinguishable 404 responses for missing and inaccessible private images.",
            "Add public/private authorization tests.",
        ], False, False),
        ("Asynchronous processing lifecycle", [
            "Keep upload response independent from AI and thumbnail work.",
            "Support PENDING, PROCESSING, COMPLETED, and FAILED states.",
            "Record processing start, completion, duration, and failure details.",
            "Define retry behavior and idempotency.",
            "Ensure processing failures never delete a successful original upload.",
        ], False, False),
        ("100 x 100 thumbnails", [
            "Generate a square 100 x 100 thumbnail with a documented crop strategy.",
            "Store thumbnails in private object storage.",
            "Save the thumbnail storage key in PostgreSQL.",
            "Expose a secured thumbnail content endpoint.",
            "Use thumbnails in galleries, search, and the user library.",
            "Test dimensions, formats, access rules, and failure handling.",
        ], False, False),
        ("Validation hardening", [
            "Verify declared MIME type matches decoded image bytes.",
            "Decide whether to support WebP or remove it from schema and documentation.",
            "Reject unreasonable dimensions and decompression-bomb inputs.",
            "Return deliberate 400, 413, 415, and storage-failure responses.",
        ], False, False),
    ],
))
story.append(PageBreak())

story.extend(section(
    "4. AI, discovery, sharing, and delivery",
    "Complete the product experience, operational safeguards, and bonus capabilities only after the core image lifecycle is stable.",
    [
        ("AI image tagging", [
            "Send original images to a vision-capable AI model asynchronously.",
            "Extract visible objects, descriptive tags, and up to three prominent colors.",
            "Require and validate a structured AI response.",
            "Store AI metadata in PostgreSQL JSONB.",
            "Record AI timing, failures, and retry outcomes.",
            "Test successful, malformed, timed-out, and failed AI responses.",
        ], False, False),
        ("Public gallery and search", [
            "Return the latest 50 public images.",
            "Search public images by objects, tags, colors, and free text.",
            "Paginate search results with stable ordering.",
            "Add only query-proven B-tree, GIN, full-text, or trigram indexes.",
            "Expose public user galleries without private metadata leakage.",
        ], False, False),
        ("Private sharing", [
            "Generate cryptographically secure share tokens.",
            "Allow owners to choose expiration times.",
            "Serve private content through valid, unexpired tokens.",
            "Allow owners to revoke links.",
            "Test expiry, revocation, ownership, and token secrecy.",
        ], False, False),
        ("Operations, documentation, and frontend", [
            "Add structured logs, request correlation, and access-denied audit events.",
            "Measure upload, storage, thumbnail, AI, search, and failure metrics.",
            "Keep OpenAPI as a living contract that follows verified design decisions.",
            "Complete comprehensive unit, integration, security, and end-to-end tests.",
            "Verify Docker, CI tests, GHCR publishing, and cloud configuration.",
            "Use AI to design and build the upload flow, owner library, public gallery, search, detail, and sharing frontend.",
            "Connect frontend requests to sessions, CSRF protection, error states, loading states, and request correlation.",
        ], False, False),
    ],
))

doc.build(story)
print(OUTPUT)

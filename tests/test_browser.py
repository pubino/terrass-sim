import threading
import unittest
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

from playwright.sync_api import (
    Browser,
    BrowserContext,
    Page,
    Playwright,
    expect,
    sync_playwright,
)

BUILD = Path(__file__).resolve().parents[1] / "build"


class BrowserTest(unittest.TestCase):
    server: ThreadingHTTPServer
    thread: threading.Thread
    playwright: Playwright
    browser: Browser
    context: BrowserContext
    page: Page

    @classmethod
    def setUpClass(cls) -> None:
        if not (BUILD / "site/index.html").is_file():
            raise RuntimeError("Build the site before running browser tests.")
        # Serve from a subdirectory to match a GitHub project Pages URL.
        cls.server = ThreadingHTTPServer(
            ("127.0.0.1", 0), partial(SimpleHTTPRequestHandler, directory=BUILD)
        )
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        cls.addClassCleanup(cls.server.server_close)
        cls.addClassCleanup(cls.thread.join, 5)
        cls.addClassCleanup(cls.server.shutdown)
        cls.playwright = sync_playwright().start()
        cls.addClassCleanup(cls.playwright.stop)
        cls.browser = cls.playwright.chromium.launch()
        cls.addClassCleanup(cls.browser.close)

    def setUp(self) -> None:
        self.context = self.browser.new_context()
        self.addCleanup(self.context.close)
        self.page = self.context.new_page()
        self.base_url = f"http://127.0.0.1:{self.server.server_port}/site/"

    def test_historical_data_highlight_and_phone_layout(self) -> None:
        self.page.goto(self.base_url)
        expect(self.page.get_by_role("status")).to_have_text(
            "Assignment complete.", timeout=60_000
        )
        expect(self.page.locator("#zip-count")).to_have_text("41,352")
        expect(self.page.locator("#territory-count")).to_have_text("500")
        self.assertTrue(self.page.locator("#distance").inner_text().endswith(" mi"))
        canvas = self.page.locator("#map")
        before = canvas.evaluate("(map) => map.toDataURL()")
        self.page.get_by_label("Highlight territory").select_option("0")
        expect(self.page.locator("#selection")).to_contain_text("ZIPs")
        self.assertNotEqual(before, canvas.evaluate("(map) => map.toDataURL()"))
        self.page.set_viewport_size({"width": 390, "height": 844})
        self.assertTrue(
            self.page.evaluate("document.documentElement.scrollWidth <= innerWidth")
        )

    def test_stop_import_and_fallback_network(self) -> None:
        self.page.route("**/data/zlls.dat", lambda route: route.abort())
        self.page.goto(self.base_url)
        expect(self.page.get_by_role("alert")).to_contain_text("Could not load")
        self.page.get_by_label("ZIP data").select_option("custom")
        self.page.get_by_role("button", name="Optimize").click()
        expect(self.page.get_by_role("alert")).to_contain_text("Choose both")
        self.page.get_by_label("Territories", exact=True).fill("2")
        self.page.get_by_label("ZIP locations (zlls.dat)").set_input_files(
            {
                "name": "locations.dat",
                "mimeType": "text/plain",
                "buffer": b"3\n01001 44 -98 AA\n01002 40 -99 AA\n01003 33 -111 BB\n",
            }
        )
        weight = self.page.get_by_label("ZIP weights (zta.dat)")
        weight.set_input_files(
            {
                "name": "weights.dat",
                "mimeType": "text/plain",
                "buffer": b"3\n01001 T 10\n01002 T 15\n01003 T 20\n",
            }
        )
        self.page.get_by_role("button", name="Optimize").click()
        expect(self.page.get_by_role("status")).to_have_text(
            "Assignment complete.", timeout=45_000
        )
        expect(self.page.locator("#zip-count")).to_have_text("3")
        expect(self.page.locator("#territory-count")).to_have_text("2")
        # Stop in the submit task so even a very fast worker cannot finish first.
        self.page.evaluate("""() => {
            document.querySelector("form").requestSubmit();
            document.querySelector("#stop").click();
        }""")
        expect(self.page.get_by_role("status")).to_contain_text("Optimization stopped")
        expect(self.page.locator("#zip-count")).to_have_text("3")
        self.page.get_by_role("button", name="Optimize").click()
        expect(self.page.get_by_role("status")).to_have_text(
            "Assignment complete.", timeout=45_000
        )

"""Sphinx configuration for Redstone Backport documentation."""

import datetime
import re
from pathlib import Path

# -- Project information -----------------------------------------------------

project = "Redstone Backport"
author = "squinchmods"
copyright_text = f"{datetime.datetime.now().year}, {author}"
copyright = copyright_text

_root = Path(__file__).resolve().parent
_gradle_props = _root.parent / "gradle.properties"
release = "0.1.0"
if _gradle_props.is_file():
    text = _gradle_props.read_text(encoding="utf-8")
    m = re.search(r"^mod_version=(.+)$", text, re.MULTILINE)
    if m:
        release = m.group(1).strip()

version = release

# -- General configuration ---------------------------------------------------

extensions = [
    "sphinx.ext.intersphinx",
    "myst_parser",
    "sphinx_copybutton",
    "sphinx_design",
    "furo.sphinxext",
    "sphinxcontrib.mermaid",
]

myst_enable_extensions = [
    "amsmath",
    "colon_fence",
    "deflist",
    "dollarmath",
    "fieldlist",
    "html_admonition",
    "html_image",
    "linkify",
    "replacements",
    "smartquotes",
    "strikethrough",
    "substitution",
    "tasklist",
]

myst_url_schemes = ["http", "https", "mailto"]

exclude_patterns = ["_build", ".venv", "redstone_backport_docs", "Thumbs.db", ".DS_Store"]

source_suffix = {
    ".rst": "restructuredtext",
    ".md": "markdown",
}

root_doc = "index"

# -- Options for HTML output -------------------------------------------------

templates_path = ["_templates"]
html_sidebars = {
    "**": [
        "sidebar/brand.html",
        "sidebar/search.html",
        "sidebar/scroll-start.html",
        "sidebar/home-link.html",
        "sidebar/navigation.html",
        "sidebar/ethical-ads.html",
        "sidebar/scroll-end.html",
        "sidebar/variant-selector.html",
    ],
}

html_theme = "furo"
html_static_path = ["_static"]
html_favicon = "icon.png"
html_css_files = ["css/custom.css"]
html_js_files = ["js/custom.js"]
html_title = f"{project} Documentation"
html_short_title = project

# Light: Dracula-inspired accents on a bright surface. Dark: classic Dracula background/foreground.
# Furo keeps the light / dark / auto toggle; these variables switch with `data-theme` / prefers-color-scheme.
html_theme_options = {
    "light_css_variables": {
        "color-background-primary": "#fafaf8",
        "color-background-secondary": "#f4f4f2",
        "color-background-border": "#e2e2dd",
        "color-foreground-primary": "#282a36",
        "color-foreground-secondary": "#44475a",
        "color-foreground-muted": "#6272a4",
        "color-foreground-border": "#6272a4",
        "color-brand-primary": "#7c3aed",
        "color-brand-content": "#7c3aed",
        "color-brand-visited": "#6d28d9",
        "color-link": "#7952b3",
        "color-link--hover": "#bd93f9",
        "color-sidebar-background": "#f0eef6",
        "color-sidebar-background-border": "#ddd8e8",
        "color-sidebar-brand-text": "#282a36",
        "color-sidebar-caption-text": "#6272a4",
        "color-sidebar-link-text": "#44475a",
        "color-sidebar-link-text--top-level": "#282a36",
        "color-sidebar-item-background--hover": "#e8e4f2",
        "color-sidebar-item-background--current": "#ddd8ec",
        "color-sidebar-item-expander-background": "transparent",
        "color-sidebar-item-expander-background--hover": "#e8e4f2",
        "color-sidebar-search-background": "#e8e4f2",
        "color-sidebar-search-foreground": "#282a36",
        "color-sidebar-search-text": "#282a36",
        "color-sidebar-search-icon": "#6272a4",
        "content-width": "1000px",
    },
    "dark_css_variables": {
        "color-background-primary": "#282a36",
        "color-background-secondary": "#21222c",
        "color-background-border": "#44475a",
        "color-foreground-primary": "#f8f8f2",
        "color-foreground-secondary": "#e2e2dc",
        "color-foreground-muted": "#6272a4",
        "color-foreground-border": "#6272a4",
        "color-brand-primary": "#bd93f9",
        "color-brand-content": "#bd93f9",
        "color-brand-visited": "#ff79c6",
        "color-link": "#8be9fd",
        "color-link--hover": "#ff79c6",
        "color-sidebar-background": "#21222c",
        "color-sidebar-background-border": "#44475a",
        "color-sidebar-brand-text": "#f8f8f2",
        "color-sidebar-caption-text": "#6272a4",
        "color-sidebar-link-text": "#c7c9d1",
        "color-sidebar-link-text--top-level": "#f8f8f2",
        "color-sidebar-item-background--hover": "#44475a",
        "color-sidebar-item-background--current": "#44475a",
        "color-sidebar-item-expander-background": "transparent",
        "color-sidebar-item-expander-background--hover": "#44475a",
        "color-sidebar-search-background": "#44475a",
        "color-sidebar-search-foreground": "#f8f8f2",
        "color-sidebar-search-text": "#f8f8f2",
        "color-sidebar-search-icon": "#bd93f9",
        "content-width": "1000px",
    },
    "sidebar_hide_name": False,
    "navigation_with_keys": True,
    "footer_icons": [],
}

pygments_style = "friendly"
pygments_dark_style = "dracula"

intersphinx_mapping = {
    "python": ("https://docs.python.org/3", None),
}

htmlhelp_basename = "RedstoneBackportDoc"


def setup(app):
    return {
        "version": release,
        "parallel_read_safe": True,
        "parallel_write_safe": True,
    }

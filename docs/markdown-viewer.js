(async function () {
  const target = document.getElementById("content");
  const markdownPath = document.body.getAttribute("data-markdown");

  if (!target || !markdownPath) return;

  try {
    const response = await fetch(markdownPath);
    if (!response.ok) {
      throw new Error("Failed to load markdown");
    }

    const markdown = await response.text();
    target.innerHTML = marked.parse(markdown);
  } catch (error) {
    target.innerHTML = "<p>Unable to load markdown page.</p>";
  }
})();

import re

with open('/app/src/main/assets/game/index.html', 'r', encoding='utf-8') as f:
    html = f.read()

# Make intro skip function
intro_pattern = r'\(function runIntro\(\) \{[\s\S]*?\}\)\(\);'
new_intro = """window.skipIntroNow = function() {
    const intro = document.getElementById('intro');
    if (intro && !intro.classList.contains('hidden')) {
        intro.classList.add('hidden');
        showMenu();
    }
};

(function runIntro() {
    const intro = document.getElementById('intro');
    const mrfeastEl = document.getElementById('intro-mrfeast');
    const projectEl = document.getElementById('intro-project');
    const presentEl = document.getElementById('intro-present');
    if (!intro) return;

    intro.addEventListener('pointerdown', skipIntroNow, { once: true });
    intro.addEventListener('click', skipIntroNow, { once: true });

    const letters = 'MrFeast'.split('');
    letters.forEach((ch, i) => {
        const s = document.createElement('span');
        s.className = 'intro-letter';
        s.textContent = ch;
        if (mrfeastEl) mrfeastEl.appendChild(s);
        setTimeout(() => s.classList.add('animate'), 100 + i * 90);
    });
    const T1 = 100 + letters.length * 90 + 100;
    setTimeout(() => { if (projectEl) projectEl.classList.add('falling'); }, T1);
    setTimeout(() => { if (presentEl) presentEl.classList.add('show'); }, T1 + 400);
    setTimeout(() => { if (presentEl) presentEl.classList.add('fade'); }, T1 + 1400);
    setTimeout(() => {
        skipIntroNow();
    }, T1 + 1800);
})();"""

html = re.sub(intro_pattern, new_intro, html, count=1)

# Also ensure btn-admin is visible in the menu for the player
html = html.replace('id="btn-admin" type="button" style="display:none;"', 'id="btn-admin" type="button"')

# Also fix the Profile button so it is never grayed out with "soon"
html = html.replace("if (!HAS_TG_API) {", "if (false) {")

with open('/app/src/main/assets/game/index.html', 'w', encoding='utf-8') as f:
    f.write(html)
print('Updated index.html successfully')

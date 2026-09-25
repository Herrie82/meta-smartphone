/* LD_PRELOAD shim for MaliitServer on athena (BlackBerry KEY2).
 *
 * The luneos-keyboard plugin treats the KEY2's Sym key as a level key: a tap
 * latches a third character level for the next physical key. BlackBerry's own
 * behaviour is different - Sym opens an on-screen symbol picker - and that is
 * what this restores, without rebuilding the plugin:
 *
 *  - HardwareKeyboard::handleKey: Sym (evdev 100, +8 in the framework's
 *    numbering) is swallowed and toggles a "symbol panel" flag instead of
 *    latching a level. Any other physical key closes the panel and then
 *    goes through untouched.
 *  - HardwareKeyboard::isSymActive: reports the flag. Only QML reads this
 *    getter (the plugin's own level logic uses the private state), so
 *    Keyboard.qml's maliit_hw_keyboard.symActive becomes "show the symbols".
 *  - HardwareKeyboard::reset (focus left the field): closes the panel.
 *
 * levelChanged() is emitted on every flag change so the QML binding updates.
 *
 * Auto-capitalisation for the physical keyboard: the plugin's own auto-caps
 * only shifts the on-screen keyboard, and its "empty field" check runs at
 * focus-in, before a web page has reported its text. Hardware letters are
 * capitalised from HardwareKeyboard::shiftLatchActive(), so answer yes there
 * whenever auto-caps is enabled for the field (setAutoCapsEnabled) and the
 * text before the cursor is empty, starts a new line, or ends a sentence.
 */
#define _GNU_SOURCE
#include <dlfcn.h>
#include <stdbool.h>
#include <stddef.h>

#define QEVENT_KEYPRESS 6
#define SYM_SCANCODE (100 + 8)
#define RESULT_CONSUMED 1
#define RESULT_TEXT 2

static void fix_lone_i(void);
static bool delete_word(int keep);
static bool word_mode, word_repeated;
static bool alt_active(const void *hwkbd);
static bool is_word_end(void *qstring);

typedef int (*handle_key_fn)(void *, int, unsigned, int, void *);
typedef void (*void_fn)(void *);

#define PLUGIN "/usr/lib/maliit/plugins/libluneos-keyboard-plugin.so"

static bool panel;

/* The plugin is dlopen()ed RTLD_LOCAL by the Maliit framework, so RTLD_NEXT
 * cannot see it; its own symbols are only reachable through its handle. */
static void *original(const char *name)
{
	static void *plugin;

	if (!plugin)
		plugin = dlopen(PLUGIN, RTLD_NOW | RTLD_NOLOAD);
	return plugin ? dlsym(plugin, name) : NULL;
}

static void set_panel(void *self, bool on)
{
	static void_fn level_changed;

	if (panel == on)
		return;
	panel = on;
	if (!level_changed)
		level_changed = (void_fn)original("_ZN14MaliitKeyboard16HardwareKeyboard12levelChangedEv");
	if (level_changed)
		level_changed(self);
}


/* Keyboard.qml writes "athena-sym-visible" into the HardwareKeyboard object's
 * objectName while the symbols panel is actually shown. */
struct qstr_ret { int *d; unsigned short *ptr; long long size; };
typedef struct qstr_ret (*object_name_fn)(const void *);
typedef void (*dealloc2_fn)(void *, long long, long long);

static bool symbols_visible(const void *obj)
{
	static object_name_fn object_name;
	static dealloc2_fn dealloc;
	static const char want[] = "athena-sym-visible";
	bool match;

	if (!object_name) {
		object_name = (object_name_fn)dlsym(RTLD_DEFAULT, "_ZNK7QObject10objectNameEv");
		dealloc = (dealloc2_fn)dlsym(RTLD_DEFAULT, "_ZN10QArrayData10deallocateEPS_xx");
	}
	if (!object_name)
		return panel;
	struct qstr_ret q = object_name(obj);
	match = q.size == (long long)(sizeof want - 1) && q.ptr;
	for (long long i = 0; match && i < q.size; i++)
		match = q.ptr[i] == (unsigned short)want[i];
	if (q.d && __atomic_sub_fetch(q.d, 1, __ATOMIC_ACQ_REL) == 0 && dealloc)
		dealloc(q.d, 2, 8);
	return match;
}

int _ZN14MaliitKeyboard16HardwareKeyboard9handleKeyEN6QEvent4TypeEj6QFlagsIN2Qt16KeyboardModifierEEP7QString(
	void *self, int type, unsigned scancode, int modifiers, void *text)
{
	static handle_key_fn real;

	if (scancode == SYM_SCANCODE) {
		if (type == QEVENT_KEYPRESS) {
			/* Toggle what is on screen, not our flag: the panel can be
			 * closed behind our back (keyboard swiped away) while the
			 * field keeps focus, and then the flag is stale. */
			if (symbols_visible(self)) {
				set_panel(self, false);
			} else {
				set_panel(self, false);	/* re-arm, so QML sees a change */
				set_panel(self, true);
			}
		}
		return RESULT_CONSUMED;
	}
	if (type == QEVENT_KEYPRESS)
		set_panel(self, false);
	if (type == QEVENT_KEYPRESS && (scancode == 57 + 8 || scancode == 28 + 8))
		fix_lone_i();		/* space or Enter ends the word */

	if (!real)
		real = (handle_key_fn)original("_ZN14MaliitKeyboard16HardwareKeyboard9handleKeyEN6QEvent4TypeEj6QFlagsIN2Qt16KeyboardModifierEEP7QString");
	if (!real)
		return 0;	/* NotHandled: the key goes to the application as is */

	/* Alt + Backspace deletes words: mark it, and let the key go through
	 * as usual - the editor hooks below turn it into a word backspace, so
	 * holding it repeats word by word on the editor's own timer. */
	if (scancode == 14 + 8 && type == QEVENT_KEYPRESS)
		word_mode = alt_active(self);

	int r = real(self, type, scancode, modifiers, text);
	if (r == RESULT_TEXT && type == QEVENT_KEYPRESS && text && is_word_end(text))
		fix_lone_i();		/* an Alt-level , . ? ! ' ends it too */
	return r;
}

bool _ZNK14MaliitKeyboard16HardwareKeyboard11isSymActiveEv(const void *self)
{
	(void)self;
	return panel;
}

void _ZN14MaliitKeyboard16HardwareKeyboard5resetEv(void *self)
{
	static void_fn real;

	if (!real)
		real = (void_fn)original("_ZN14MaliitKeyboard16HardwareKeyboard5resetEv");
	if (real)
		real(self);
	set_panel(self, false);
}

/* Web text fields reach Maliit without an auto-capitalisation hint, so the
 * host answers "no" and the plugin never arms auto-caps. When the host says
 * no, turn it on anyway for plain free-text fields - never for password
 * (hidden) fields, or for number, phone, e-mail or URL fields. */
#include <stdio.h>

#define MAPLUGINS "libmaliit-plugins.so.0"
enum { CONTENT_FREE_TEXT = 0 };

static void *host;		/* MInputMethodHost, seen in autoCapitalizationEnabled */
typedef bool (*host_bool_fn)(void *, bool *);
typedef int (*host_int_fn)(void *, bool *);

static void *maliit_sym(const char *name)
{
	void *s = dlsym(RTLD_NEXT, name);
	if (!s) {
		void *h = dlopen(MAPLUGINS, RTLD_NOW | RTLD_NOLOAD);
		s = h ? dlsym(h, name) : NULL;
	}
	return s;
}

bool _ZN16MInputMethodHost25autoCapitalizationEnabledERb(void *self, bool *valid)
{
	static host_bool_fn real, hidden;
	static host_int_fn content;
	bool v;
	bool r;

	if (!real) {
		real = (host_bool_fn)maliit_sym("_ZN16MInputMethodHost25autoCapitalizationEnabledERb");
		hidden = (host_bool_fn)maliit_sym("_ZN16MInputMethodHost10hiddenTextERb");
		content = (host_int_fn)maliit_sym("_ZN16MInputMethodHost11contentTypeERb");
	}
	host = self;
	r = real ? real(self, valid) : false;
	if (r)
		return true;
	v = false;
	if (hidden && hidden(self, &v) && v)
		return false;
	v = false;
	if (content) {
		int ct = content(self, &v);
		if (v && ct != CONTENT_FREE_TEXT)
			return false;
	}
	fprintf(stderr, "athena-symkey: host gave no auto-caps hint; enabling for a free-text field\n");
	*valid = true;
	return true;
}

/* --- auto-caps for hardware letters -------------------------------------- */

static bool autocaps_enabled;	/* the editor's final say, incl. the user setting */

typedef void (*set_bool_fn)(void *, bool);

void _ZN14MaliitKeyboard18AbstractTextEditor18setAutoCapsEnabledEb(void *self, bool on)
{
	static set_bool_fn real;

	if (!real)
		real = (set_bool_fn)original("_ZN14MaliitKeyboard18AbstractTextEditor18setAutoCapsEnabledEb");
	autocaps_enabled = on;
	if (real)
		real(self, on);
}

/* Qt 6 QString: {QArrayData *d; char16_t *ptr; qsizetype size} */
struct qstring { int *d; unsigned short *ptr; long long size; };
typedef bool (*surr_fn)(void *, struct qstring *, int *);
typedef void (*dealloc_fn)(void *, long long, long long);

static bool sentence_start(void)
{
	static surr_fn surrounding;
	static dealloc_fn dealloc;
	struct qstring q = {0, 0, 0};
	int pos = -1;
	bool cap = false;

	if (!host)
		return false;
	if (!surrounding) {
		surrounding = (surr_fn)maliit_sym("_ZN16MInputMethodHost15surroundingTextER7QStringRi");
		dealloc = (dealloc_fn)dlsym(RTLD_DEFAULT, "_ZN10QArrayData10deallocateEPS_xx");
	}
	if (surrounding && surrounding(host, &q, &pos) && pos >= 0 && pos <= q.size) {
		int i = pos - 1;
		bool space = false, newline = false;

		while (i >= 0 && (q.ptr[i] == ' ' || q.ptr[i] == '\t' || q.ptr[i] == '\n' ||
				  q.ptr[i] == '\r' || q.ptr[i] == 0xa0)) {
			if (q.ptr[i] == '\n' || q.ptr[i] == '\r')
				newline = true;
			space = true;
			i--;
		}
		if (i < 0 || newline)
			cap = true;
		else if (space && (q.ptr[i] == '.' || q.ptr[i] == '!' || q.ptr[i] == '?'))
			cap = true;
	}
	if (q.d && __atomic_sub_fetch(q.d, 1, __ATOMIC_ACQ_REL) == 0 && dealloc)
		dealloc(q.d, 2, 8);
	return cap;
}

typedef bool (*const_bool_fn)(const void *);

bool _ZNK14MaliitKeyboard16HardwareKeyboard16shiftLatchActiveEv(const void *self)
{
	static const_bool_fn real;

	if (!real)
		real = (const_bool_fn)original("_ZNK14MaliitKeyboard16HardwareKeyboard16shiftLatchActiveEv");
	if (real && real(self))
		return true;
	return autocaps_enabled && sentence_start();
}

/* --- "i" -> "I" ------------------------------------------------------------ */

typedef void (*commit_fn)(void *, const struct qstring *, int, int, int);
struct qbytearrayview { long long size; const char *data; };
typedef struct qstring (*from_utf8_fn)(struct qbytearrayview);

static void qstring_release(struct qstring *q)
{
	static dealloc_fn dealloc;

	if (!dealloc)
		dealloc = (dealloc_fn)dlsym(RTLD_DEFAULT, "_ZN10QArrayData10deallocateEPS_xx");
	if (q->d && __atomic_sub_fetch(q->d, 1, __ATOMIC_ACQ_REL) == 0 && dealloc)
		dealloc(q->d, 2, 8);
}

static bool is_word_end(void *text)
{
	struct qstring *q = text;

	if (q->size != 1 || !q->ptr)
		return false;
	switch (q->ptr[0]) {
	case ',': case '.': case '?': case '!': case '\'': case ';': case ':':
		return true;
	}
	return false;
}

/* The word just typed is a lone lower-case "i": swap it for "I" in place.
 * Only where auto-caps is on (free text, not a password), like the rest. */
static void fix_lone_i(void)
{
	static surr_fn surrounding;
	static commit_fn commit;
	static from_utf8_fn from_utf8;
	struct qstring q = {0, 0, 0};
	int pos = -1;
	bool lone = false;

	if (!autocaps_enabled || !host)
		return;
	if (!surrounding) {
		surrounding = (surr_fn)maliit_sym("_ZN16MInputMethodHost15surroundingTextER7QStringRi");
		commit = (commit_fn)maliit_sym("_ZN16MInputMethodHost16sendCommitStringERK7QStringiii");
		from_utf8 = (from_utf8_fn)dlsym(RTLD_DEFAULT, "_ZN7QString8fromUtf8E14QByteArrayView");
	}
	if (!surrounding || !commit || !from_utf8)
		return;
	if (surrounding(host, &q, &pos) && pos >= 1 && pos <= q.size && q.ptr[pos - 1] == 'i') {
		unsigned short before = pos >= 2 ? q.ptr[pos - 2] : ' ';
		lone = before == ' ' || before == '\t' || before == '\n' || before == '\r' ||
		       before == 0xa0 || before == '"' || before == '(' || before == 0x201c;
	}
	qstring_release(&q);
	if (lone) {
		struct qbytearrayview v = {1, "I"};
		struct qstring cap = from_utf8(v);
		commit(host, &cap, -1, 1, -1);	/* replace the one character before the cursor */
		qstring_release(&cap);
	}
}

/* --- arrows from Alt + keyboard-surface drags ------------------------------
 * athena-kbd-scroll turns Alt + a slide on the keys into arrow keys. Alt is
 * physically held, so they arrive as Alt+arrow, which the plugin passes back
 * to the application unchanged - and Alt+Left/Right is "back"/"forward" in a
 * browser. The profile owns Alt as a character level, so strip it here. */
#define QT_KEY_LEFT 0x01000012
#define QT_KEY_DOWN 0x01000015
#define QT_ALT_MODIFIER 0x08000000

typedef void (*pke_fn)(void *, int, int, int, void *, bool, int, unsigned, unsigned, unsigned long);

void _ZN20MAbstractInputMethod15processKeyEventEN6QEvent4TypeEN2Qt3KeyE6QFlagsINS2_16KeyboardModifierEERK7QStringbijjm(
	void *self, int type, int key, int modifiers, void *text, bool autorepeat, int count,
	unsigned scancode, unsigned native_modifiers, unsigned long time)
{
	static pke_fn real;

	if (!real)
		real = (pke_fn)maliit_sym("_ZN20MAbstractInputMethod15processKeyEventEN6QEvent4TypeEN2Qt3KeyE6QFlagsINS2_16KeyboardModifierEERK7QStringbijjm");
	if (key >= QT_KEY_LEFT && key <= QT_KEY_DOWN) {
		modifiers &= ~QT_ALT_MODIFIER;
		native_modifiers &= ~0x8u;	/* xkb Mod1 */
	}
	if (real)
		real(self, type, key, modifiers, text, autorepeat, count, scancode, native_modifiers, time);
}

/* --- Alt + Backspace: delete a word --------------------------------------- */

static bool alt_active(const void *hwkbd)
{
	static const_bool_fn is_alt;

	if (!is_alt)
		is_alt = (const_bool_fn)original("_ZNK14MaliitKeyboard16HardwareKeyboard11isAltActiveEv");
	return is_alt && is_alt(hwkbd);
}

static bool is_space16(unsigned short c)
{
	return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == 0xa0;
}

/* Deletes back to the start of the previous word - any spaces before the
 * cursor, then the run of non-space characters before them - except the
 * first `keep` characters, which the editor's own backspace then removes. */
static bool delete_word(int keep)
{
	static surr_fn surrounding;
	static commit_fn commit;
	struct qstring q = {0, 0, 0}, empty = {0, 0, 0};
	int pos = -1, n = 0;

	if (!host)
		return false;
	if (!surrounding) {
		surrounding = (surr_fn)maliit_sym("_ZN16MInputMethodHost15surroundingTextER7QStringRi");
		commit = (commit_fn)maliit_sym("_ZN16MInputMethodHost16sendCommitStringERK7QStringiii");
	}
	if (!surrounding || !commit)
		return false;
	if (!surrounding(host, &q, &pos) || pos < 0 || pos > q.size) {
		qstring_release(&q);
		return false;
	}
	int i = pos;
	while (i > 0 && is_space16(q.ptr[i - 1])) i--;
	while (i > 0 && !is_space16(q.ptr[i - 1])) i--;
	n = pos - i - keep;
	qstring_release(&q);
	if (n > 0)
		commit(host, &empty, -n, n, -1);
	return true;
}

/* Alt + Backspace, held or tapped.
 *
 * The editor's own backspace sends a Backspace key event back to the
 * application; with Alt physically held that arrives as Alt+Backspace, which
 * applications ignore - so the editor's repeat ticked but deleted nothing.
 * Its repeat timer is still the right rhythm, so keep it: each tick deletes a
 * word here, by text replacement, and a tap deletes one on release. While the
 * editor's backspace code runs from these hooks, the key events it sends are
 * dropped (MInputMethodHost::sendKeyEvent), so nothing is deleted twice in an
 * application that does understand Alt+Backspace. */
typedef void (*editor_key_fn)(void *, const void *);
typedef void (*send_key_fn)(void *, const void *, int);
static bool drop_keys;

void _ZN16MInputMethodHost12sendKeyEventERK9QKeyEventN6Maliit16EventRequestTypeE(void *self, const void *ev, int req)
{
	static send_key_fn real;

	if (!real)
		real = (send_key_fn)maliit_sym("_ZN16MInputMethodHost12sendKeyEventERK9QKeyEventN6Maliit16EventRequestTypeE");
	if (drop_keys)
		return;
	if (real)
		real(self, ev, req);
}

void _ZN14MaliitKeyboard18AbstractTextEditor12onKeyPressedERKNS_3KeyE(void *self, const void *key)
{
	static editor_key_fn real;

	if (!real)
		real = (editor_key_fn)original("_ZN14MaliitKeyboard18AbstractTextEditor12onKeyPressedERKNS_3KeyE");
	if (word_mode)
		word_repeated = false;
	if (real)
		real(self, key);
}

void _ZN14MaliitKeyboard18AbstractTextEditor19autoRepeatBackspaceEv(void *self)
{
	static void_fn real;

	if (!real)
		real = (void_fn)original("_ZN14MaliitKeyboard18AbstractTextEditor19autoRepeatBackspaceEv");
	if (word_mode) {
		word_repeated = true;
		delete_word(0);
		drop_keys = true;	/* the editor's own delete, and its timer restart */
	}
	if (real)
		real(self);
	drop_keys = false;
}

void _ZN14MaliitKeyboard18AbstractTextEditor13onKeyReleasedERKNS_3KeyE(void *self, const void *key)
{
	static editor_key_fn real;

	if (!real)
		real = (editor_key_fn)original("_ZN14MaliitKeyboard18AbstractTextEditor13onKeyReleasedERKNS_3KeyE");
	if (word_mode) {
		if (!word_repeated)
			delete_word(0);	/* a tap */
		drop_keys = true;
	}
	if (real)
		real(self, key);
	drop_keys = false;
	word_mode = false;
}

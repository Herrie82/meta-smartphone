/* athena-kbd-scroll - scroll by sliding a finger over the BlackBerry KEY2's
 * capacitive keyboard, as on its Android.
 *
 * The keys double as a touch surface ("touch_keypad", Synaptics, 1080x525,
 * protocol B) that nothing on LuneOS reads. A one-finger, mostly vertical
 * slide that no key press accompanies is replayed as a finger drag on the
 * touchscreen, so every toolkit (Mojo, Enyo, QML, Chromium) scrolls - and
 * flings - exactly as it would under a real finger. The injected contact
 * uses its own slot and tracking id, so a finger on the screen is not
 * disturbed. Typing always wins: a key press during the touch, or just
 * before it, cancels the gesture.
 *
 * With Alt held, the same slide moves the text cursor instead: one arrow key
 * per step, sent through the nav_key device (the only one here that has
 * arrow keys).
 *
 *   athena-kbd-scroll [kbd-touch-dev] [keys-dev] [screen-dev] [arrow-dev]
 * Sideways slides drag sideways (launcher tabs, carousels), each slide locked
 * to the axis it started on - except in a text field (MaliitServer's shim
 * keeps /run/athena-text-focus up to date), where a right-to-left slide
 * deletes the word before the cursor: Alt+Backspace on the keyboard device,
 * the same word delete as the physical keys.
 * Tunables (environment): KBDSCROLL_SCALE (screen px per pad unit, 3.0),
 * KBDSCROLL_SCALE_X (the same for sideways slides, 1.5),
 * KBDSCROLL_SLOP (pad units before a slide counts, 55),
 * KBDSCROLL_TYPING_MS (key-press quiet time needed before a touch, 300),
 * KBDCURSOR_STEP_X / KBDCURSOR_STEP_Y (pad units per arrow key, 40 / 95),
 * KBDSCROLL_DEBUG=1 logs contacts and decisions.
 *
 * Cursor mode: the thumb holding Alt rests on the capacitive surface too, so
 * the cursor follows whichever contact moves first (DRIVE_SLOP) - never the
 * resting one - and each slide is locked to the axis it started on, so jitter
 * on the other axis cannot add stray up/down steps.
 */
#include <errno.h>
#include <fcntl.h>
#include <linux/input.h>
#include <poll.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

#define SLOTS 10
#define INJ_SLOT 9
#define INJ_ID 31337

static int scr_fd, scr_w = 1080, scr_h = 1620;
static double scale = 3.0, scale_x = 1.5;
static int slop = 55, typing_ms = 300;
static int step_x = 40, step_y = 95, debug;
#define DRIVE_SLOP 25
#define dbg(...) do { if (debug) fprintf(stderr, __VA_ARGS__); } while (0)
static int arrow_fd = -1;
static int keys_wfd = -1;
static long long own_keys_until;	/* our own Alt+Backspace, echoed back */

static long long now_ms(void);

static int text_focus(void)
{
	char b[4] = "";
	int fd = open("/run/athena-text-focus", O_RDONLY);
	if (fd < 0)
		return 0;
	if (read(fd, b, sizeof b - 1) < 0) { b[0] = 0; }
	close(fd);
	return b[0] == '1';
}

static void key_event(int code, int value)
{
	struct input_event e[2];
	memset(e, 0, sizeof e);
	e[0].type = EV_KEY; e[0].code = code; e[0].value = value;
	e[1].type = EV_SYN;
	if (keys_wfd >= 0 && write(keys_wfd, e, sizeof e) < 0) { /* ignore */ }
}

static void delete_word(void)
{
	own_keys_until = now_ms() + 150;
	key_event(KEY_LEFTALT, 1);
	key_event(KEY_BACKSPACE, 1);
	key_event(KEY_BACKSPACE, 0);
	key_event(KEY_LEFTALT, 0);
}

static void arrow(int code)
{
	struct input_event e[4];
	memset(e, 0, sizeof e);
	e[0].type = EV_KEY; e[0].code = code; e[0].value = 1;
	e[1].type = EV_SYN;
	e[2].type = EV_KEY; e[2].code = code; e[2].value = 0;
	e[3].type = EV_SYN;
	if (arrow_fd >= 0 && write(arrow_fd, e, sizeof e) < 0) { /* ignore */ }
}

static long long now_ms(void)
{
	struct timespec t;
	clock_gettime(CLOCK_MONOTONIC, &t);
	return t.tv_sec * 1000LL + t.tv_nsec / 1000000;
}

static void emit(int type, int code, int value)
{
	struct input_event e;
	memset(&e, 0, sizeof e);
	e.type = type;
	e.code = code;
	e.value = value;
	if (write(scr_fd, &e, sizeof e) < 0) { /* nothing useful to do */ }
}

static int inj_down, inj_x, inj_y;

static void inj_touch(int x, int y)
{
	emit(EV_ABS, ABS_MT_SLOT, INJ_SLOT);
	if (!inj_down) {
		emit(EV_ABS, ABS_MT_TRACKING_ID, INJ_ID);
		emit(EV_ABS, ABS_MT_TOUCH_MAJOR, 8);
		emit(EV_ABS, ABS_MT_PRESSURE, 60);
		/* The kernel drops a value equal to the slot's last one, and a
		 * compositor started since then never saw it (it takes 0): a
		 * touch-down at the previous touch-down's spot would land at the
		 * top-left. Step off the value first so both always get through. */
		emit(EV_ABS, ABS_MT_POSITION_X, x + 1);
		emit(EV_ABS, ABS_MT_POSITION_Y, y + 1);
	}
	emit(EV_ABS, ABS_MT_POSITION_X, x);
	emit(EV_ABS, ABS_MT_POSITION_Y, y);
	if (!inj_down) {
		emit(EV_KEY, BTN_TOUCH, 1);
		emit(EV_KEY, BTN_TOOL_FINGER, 1);
	}
	emit(EV_SYN, SYN_REPORT, 0);
	inj_down = 1;
	inj_x = x;
	inj_y = y;
}

static void inj_up(void)
{
	if (!inj_down)
		return;
	emit(EV_ABS, ABS_MT_SLOT, INJ_SLOT);
	emit(EV_ABS, ABS_MT_TRACKING_ID, -1);
	emit(EV_KEY, BTN_TOUCH, 0);
	emit(EV_KEY, BTN_TOOL_FINGER, 0);
	emit(EV_SYN, SYN_REPORT, 0);
	inj_down = 0;
}

static int display_on(void)
{
	char b[16] = "";
	int fd = open("/sys/class/leds/lcd-backlight/brightness", O_RDONLY);
	if (fd < 0)
		return 1;
	if (read(fd, b, sizeof b - 1) < 0) { b[0] = 0; }
	close(fd);
	return atoi(b) > 0;
}

int main(int argc, char **argv)
{
	const char *pad_dev = argc > 1 ? argv[1] : "/dev/input/event1";
	const char *key_dev = argc > 2 ? argv[2] : "/dev/input/event2";
	const char *scr_dev = argc > 3 ? argv[3] : "/dev/input/event3";
	const char *arrow_dev = argc > 4 ? argv[4] : "/dev/input/event5";
	int alt_down = 0, was_alt = 0, alt_block = 0;
	int drv = -1, axis = 0, ax = 0, ay = 0;	/* cursor-mode driver contact */
	int sx[SLOTS], sy[SLOTS], fresh[SLOTS];
	int pad_fd, key_fd;
	struct input_absinfo ai;
	int slot = 0, id[SLOTS], x[SLOTS], y[SLOTS];
	long long last_key = 0;
	/* gesture state for the one tracked finger */
	int tracking = 0, rejected = 0, scrolling = 0;
	int t_slot = -1, x0 = 0, y0 = 0, anchor_x = 0, anchor_y = 0, pad_anchor = 0, scroll_axis = 'y';
	char *v;

	if ((v = getenv("KBDSCROLL_SCALE"))) scale = atof(v);
	if ((v = getenv("KBDSCROLL_SCALE_X"))) scale_x = atof(v);
	if ((v = getenv("KBDSCROLL_SLOP"))) slop = atoi(v);
	if ((v = getenv("KBDSCROLL_TYPING_MS"))) typing_ms = atoi(v);
	if ((v = getenv("KBDCURSOR_STEP_X"))) step_x = atoi(v);
	if ((v = getenv("KBDCURSOR_STEP_Y"))) step_y = atoi(v);
	if ((v = getenv("KBDSCROLL_DEBUG"))) debug = atoi(v);
	arrow_fd = open(arrow_dev, O_WRONLY);

	pad_fd = open(pad_dev, O_RDONLY | O_NONBLOCK);
	key_fd = open(key_dev, O_RDONLY | O_NONBLOCK);
	keys_wfd = open(key_dev, O_WRONLY);
	scr_fd = open(scr_dev, O_WRONLY);
	if (pad_fd < 0 || key_fd < 0 || scr_fd < 0) {
		perror("open");
		return 1;
	}
	if (ioctl(scr_fd, EVIOCGABS(ABS_MT_POSITION_X), &ai) == 0) scr_w = ai.maximum + 1;
	if (ioctl(scr_fd, EVIOCGABS(ABS_MT_POSITION_Y), &ai) == 0) scr_h = ai.maximum + 1;
	for (int i = 0; i < SLOTS; i++) { id[i] = -1; fresh[i] = 0; sx[i] = sy[i] = x[i] = y[i] = 0; }
	fprintf(stderr, "athena-kbd-scroll: pad %s keys %s screen %s (%dx%d) arrows %s%s scale %.2f slop %d\n",
		pad_dev, key_dev, scr_dev, scr_w, scr_h, arrow_dev, arrow_fd < 0 ? " (unavailable)" : "", scale, slop);

	struct pollfd pf[2] = {{pad_fd, POLLIN, 0}, {key_fd, POLLIN, 0}};
	for (;;) {
		if (poll(pf, 2, -1) < 0) {
			if (errno == EINTR) continue;
			return 1;
		}
		if (pf[1].revents & POLLIN) {
			struct input_event e;
			while (read(key_fd, &e, sizeof e) == sizeof e) {
				if (now_ms() < own_keys_until)
					continue;	/* our own word delete coming back */
				if (e.type == EV_KEY && (e.code == KEY_LEFTALT || e.code == KEY_RIGHTALT)) {
					if (e.value != 2)
						alt_down = e.value;
					continue;	/* Alt is the cursor modifier, not typing */
				}
				if (e.type == EV_KEY && e.value == 1) {
					last_key = now_ms();
					if (tracking && !scrolling)
						rejected = 1;	/* typing, not a slide */
				}
			}
		}
		if (!(pf[0].revents & POLLIN))
			continue;

		struct input_event e;
		while (read(pad_fd, &e, sizeof e) == sizeof e) {
		if (e.type == EV_ABS) {
			switch (e.code) {
			case ABS_MT_SLOT: if (e.value >= 0 && e.value < SLOTS) slot = e.value; break;
			case ABS_MT_TRACKING_ID:
				if (e.value >= 0 && id[slot] < 0) fresh[slot] = 1;
				id[slot] = e.value;
				break;
			case ABS_MT_POSITION_X: x[slot] = e.value; break;
			case ABS_MT_POSITION_Y: y[slot] = e.value; break;
			}
			continue;
		}
		if (e.type != EV_SYN || e.code != SYN_REPORT)
			continue;

		int active = 0, first = -1;
		for (int i = 0; i < SLOTS; i++) {
			if (id[i] < 0) continue;
			if (fresh[i]) { sx[i] = x[i]; sy[i] = y[i]; fresh[i] = 0; }
			active++;
			if (first < 0) first = i;
		}

		/* ---- Alt held: cursor mode ---- */
		if (alt_down) {
			if (!was_alt) {
				/* Alt just went down: whatever moved before does not count */
				for (int i = 0; i < SLOTS; i++) { sx[i] = x[i]; sy[i] = y[i]; }
				drv = -1;
				was_alt = 1;
				inj_up();
				tracking = scrolling = 0;
				dbg("alt down, %d contact(s)\n", active);
			}
			if (drv >= 0 && id[drv] < 0) {
				dbg("driver %d lifted\n", drv);
				drv = -1;
			}
			if (drv < 0) {
				int best = DRIVE_SLOP;
				for (int i = 0; i < SLOTS; i++) {
					if (id[i] < 0) continue;
					int d = abs(x[i] - sx[i]) > abs(y[i] - sy[i]) ? abs(x[i] - sx[i]) : abs(y[i] - sy[i]);
					if (d >= best) { best = d; drv = i; }
				}
				if (drv >= 0) {
					axis = abs(x[drv] - sx[drv]) >= abs(y[drv] - sy[drv]) ? 'x' : 'y';
					ax = sx[drv];
					ay = sy[drv];
					dbg("driver slot %d axis %c from %d,%d\n", drv, axis, ax, ay);
				}
			}
			if (drv >= 0 && display_on()) {
				if (axis == 'x') {
					while (x[drv] - ax >= step_x) { arrow(KEY_RIGHT); ax += step_x; }
					while (ax - x[drv] >= step_x) { arrow(KEY_LEFT); ax -= step_x; }
				} else {
					while (y[drv] - ay >= step_y) { arrow(KEY_DOWN); ay += step_y; }
					while (ay - y[drv] >= step_y) { arrow(KEY_UP); ay -= step_y; }
				}
			}
			continue;
		}
		if (was_alt) {
			/* Alt released: the fingers on the pad were steering the cursor;
			 * none of them may turn into a scroll */
			was_alt = 0;
			drv = -1;
			alt_block = active > 0;
			dbg("alt up\n");
		}
		if (alt_block) {
			if (active == 0) alt_block = 0;
			continue;
		}

		/* ---- scroll mode ---- */
		if (!tracking) {
			if (active == 1) {
				tracking = 1;
				scrolling = 0;
				t_slot = first;
				x0 = x[first];
				y0 = y[first];
				rejected = (now_ms() - last_key < typing_ms) || !display_on();
				dbg("touch at %d,%d%s\n", x0, y0, !rejected ? "" :
				    now_ms() - last_key < typing_ms ? ": ignored, a key was just pressed" : ": ignored, display off");
			}
			continue;
		}

		/* finger lifted, or a second one arrived: the gesture is over */
		if (active == 0 || id[t_slot] < 0 || active > 1) {
			if (active > 1 && !rejected)
				dbg("second contact: gesture cancelled\n");
			else if (active == 0)
				dbg("lifted%s\n", scrolling ? "" : rejected ? " (was ignored)" : " before moving past the slop");
			inj_up();
			tracking = scrolling = 0;
			if (active > 1) rejected = 1;
			if (active == 0) rejected = 0;
			continue;
		}
		if (rejected)
			continue;

		int dx = x[t_slot] - x0, dy = y[t_slot] - y0;
		if (!scrolling) {
			if (abs(dx) < slop && abs(dy) < slop)
				continue;
			/* lock the slide to the axis it started on */
			scroll_axis = abs(dx) > abs(dy) ? 'x' : 'y';
			if (scroll_axis == 'x' && dx < 0 && text_focus()) {
				dbg("slide left in a text field: delete word\n");
				delete_word();
				rejected = 1;	/* one word per slide */
				continue;
			}
			dbg("slide %s\n", scroll_axis == 'x' ? (dx < 0 ? "left" : "right") : (dy < 0 ? "up" : "down"));
			scrolling = 1;
			/* A sideways drag starts on the side it moves away from, so it
			 * has most of the screen to travel: the launcher only changes
			 * tab on a slow drag once it has covered half the width. */
			anchor_x = scroll_axis == 'x' ? (dx < 0 ? scr_w - scr_w / 10 : scr_w / 10) : scr_w / 2;
			anchor_y = scr_h / 2;
			pad_anchor = scroll_axis == 'x' ? x[t_slot] : y[t_slot];
			inj_touch(anchor_x, anchor_y);
			continue;
		}

		if (scroll_axis == 'x') {
			/* sideways: the launcher's tabs, horizontal lists */
			/* Hold at the screen edge rather than lifting and taking hold
			 * again: a second touch would stop the tab change the first one
			 * started. */
			int sx = anchor_x + (int)((x[t_slot] - pad_anchor) * scale_x);
			if (sx < scr_w / 20) sx = scr_w / 20;
			if (sx > scr_w - scr_w / 20) sx = scr_w - scr_w / 20;
			inj_touch(sx, anchor_y);
			continue;
		}

		int sy = anchor_y + (int)((y[t_slot] - pad_anchor) * scale);
		if (sy < scr_h / 8 || sy > scr_h - scr_h / 8) {
			/* ran out of screen: lift, and take hold again mid-screen */
			inj_up();
			anchor_y = scr_h / 2;
			pad_anchor = y[t_slot];
			inj_touch(scr_w / 2, anchor_y);
			continue;
		}
		inj_touch(scr_w / 2, sy);
		}
	}
}

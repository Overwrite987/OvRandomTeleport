package ru.overwrite.rtp.utils.color;

import ru.overwrite.rtp.utils.Colorizer;

public class LegacyAdvancedColorizer implements Colorizer {

    private static final char COLOR_CHAR = '§';

    @Override
    public String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        final StringBuilder b = new StringBuilder();
        final char[] mess = message.toCharArray();
        boolean color = false, hashtag = false, doubleTag = false;
        char tmp;
        for (int i = 0; i < mess.length; ) {
            final char c = mess[i];
            if (doubleTag) {
                doubleTag = false;
                final int max = i + 3;
                if (max <= mess.length) {
                    boolean match = true;
                    for (int n = i; n < max; n++) {
                        tmp = mess[n];
                        if (!((tmp >= '0' && tmp <= '9') || (tmp >= 'a' && tmp <= 'f') || (tmp >= 'A' && tmp <= 'F'))) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        b.append(COLOR_CHAR);
                        b.append('x');
                        for (; i < max; i++) {
                            tmp = mess[i];
                            b.append(COLOR_CHAR);
                            b.append(tmp);

                            b.append(COLOR_CHAR);
                            b.append(tmp);
                        }
                        continue;
                    }
                }
                b.append('&');
                b.append("##");
            }
            if (hashtag) {
                hashtag = false;
                // Check for double hashtag (&##123 => &#112233)
                if (c == '#') {
                    doubleTag = true;
                    i++;
                    continue;
                }
                final int max = i + 6;
                if (max <= mess.length) {
                    boolean match = true;
                    for (int n = i; n < max; n++) {
                        tmp = mess[n];
                        if (!((tmp >= '0' && tmp <= '9') || (tmp >= 'a' && tmp <= 'f') || (tmp >= 'A' && tmp <= 'F'))) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        b.append(COLOR_CHAR);
                        b.append('x');

                        for (; i < max; i++) {
                            b.append(COLOR_CHAR);
                            b.append(mess[i]);
                        }
                        continue;
                    }
                }
                b.append('&');
                b.append('#');
            }
            if (color) {
                color = false;
                if (c == '#') {
                    hashtag = true;
                    i++;
                    continue;
                }
                if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || c == 'r' || (c >= 'k' && c <= 'o') || (c >= 'A' && c <= 'F') || c == 'R' || (c >= 'K' && c <= 'O')) {
                    b.append(COLOR_CHAR);
                    b.append(c);
                    i++;
                    continue;
                }
                b.append('&');
            }
            if (c == '&') {
                color = true;
                i++;
                continue;
            }
            b.append(c);
            i++;
        }
        if (color) {
            b.append('&');
        } else if (hashtag) {
            b.append('&');
            b.append('#');
        } else if (doubleTag) {
            b.append('&');
            b.append("##");
        }
        return b.toString();
    }
}

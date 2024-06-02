package ru.overwrite.rtp.actions;

import lombok.Getter;
import lombok.Setter;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Action {
	
	private static final Pattern ACTION_PATTERN = Pattern.compile("\\[(\\w+)\\](?: ?(.*))");
	
	@Getter
	@Setter
	private ActionType type;
	
	@Getter
	private String context;
	
	public Action(ActionType type, String context) {
		this.type = type;
		this.context = context;
	}
	
	public static Action fromString(String str) {
        Matcher matcher = ACTION_PATTERN.matcher(str);
        if (!matcher.matches()) return null;
        ActionType type = ActionType.fromString(matcher.group(1));
        if (type == null) return null;
        return new Action(type, matcher.group(2));
    }

}

package ru.overwrite.rtp.actions;

import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public enum ActionType {
	
	MESSAGE,
	SOUND,
	TITLE,
	EFFECT,
	CONSOLE;
	
	private static final Map<String, ActionType> BY_NAME = Stream.of(values()).collect(Collectors.toMap(Enum::name, en -> en));
    
    public static ActionType fromString(String str) {
        return BY_NAME.get(str.toUpperCase());
    }

}

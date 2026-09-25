package com.wayfarer;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum StripPlacement
{
	MOVABLE("Movable (Alt-drag)"),
	ABOVE_CHAT("Centred above chat");

	private final String label;

	@Override
	public String toString()
	{
		return label;
	}
}

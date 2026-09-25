package com.wayfarer;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum StripShape
{
	PILL("Pill"),
	SQUARE("Square");

	private final String label;

	@Override
	public String toString()
	{
		return label;
	}
}

package com.roeper.bu.urop.readings;

import com.google.common.base.Optional;
import com.roeper.bu.urop.lib.Service;

public interface ReadingProvider<T> extends Service
{
	Optional<T> getReading();
}

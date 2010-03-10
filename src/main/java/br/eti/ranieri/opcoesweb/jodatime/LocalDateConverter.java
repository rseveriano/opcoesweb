package br.eti.ranieri.opcoesweb.jodatime;

import java.util.Locale;

import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.joda.time.ReadablePartial;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class LocalDateConverter implements IConverter {

	public static final LocalDateConverter DEFAULT_CONVERTER = new LocalDateConverter();

	DateTimeFormatter formatter = DateTimeFormat.forPattern("dd/MM/yyyy")
			.withLocale(new Locale("pt", "BR"));

	LocalDateConverter() {
	}

	public Object convertToObject(String value, Locale locale) {
		try {
			return formatter.parseDateTime(value).toLocalDate();
		} catch (IllegalArgumentException e) {
			throw new ConversionException("'" + value
					+ "' is not a valid date, use 'dd/MM/yyyy'", e);
		}
	}

	public String convertToString(Object value, Locale locale) {
		if (value == null)
			return null;
		return formatter.print((ReadablePartial) value);
	}

}

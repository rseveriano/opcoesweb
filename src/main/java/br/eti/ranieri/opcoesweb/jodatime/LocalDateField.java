package br.eti.ranieri.opcoesweb.jodatime;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.convert.IConverter;
import org.joda.time.LocalDate;

public class LocalDateField extends TextField<LocalDate> {

	public LocalDateField(String id) {
		super(id);
	}

	@Override
	public IConverter getConverter(Class<?> type) {
		return LocalDateConverter.DEFAULT_CONVERTER;
	}
}

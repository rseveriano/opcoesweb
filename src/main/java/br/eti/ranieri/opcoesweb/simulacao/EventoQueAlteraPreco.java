package br.eti.ranieri.opcoesweb.simulacao;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class EventoQueAlteraPreco implements Serializable {

	boolean selecionado = true;
	LocalDate data;
	private AlteracaoCustodia alteracao;

	static final transient Locale ptBR = new Locale("pt", "BR");
	static final transient DateTimeFormatter dateFormatter = DateTimeFormat
			.forPattern("dd/MM/yyyy").withLocale(ptBR);
	static final transient NumberFormat numberFormatter = DecimalFormat
			.getCurrencyInstance(ptBR);

	public EventoQueAlteraPreco(LocalDate data, AlteracaoCustodia alteracao) {
		this.data = data;
		this.alteracao = alteracao;
	}

	public String getDataFormatada() {
		if (data == null)
			return "";
		return dateFormatter.print(data);
	}

	public String getDescricao() {
		if (TipoAlteracaoCustodia.PROVENTOS.equals(alteracao.getTipo()))
			return alteracao.getTipo() + " de " + numberFormatter.format(alteracao.getValor());
		return alteracao.getTipo() + " em " + alteracao.getValor();
	}

	public boolean isSelecionado() {
		return selecionado;
	}

	public void setSelecionado(boolean selecionado) {
		this.selecionado = selecionado;
	}

}

package br.eti.ranieri.opcoesweb.estado;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.LocalDate;

public enum Serie {

	A //
	, B //
	, C //
	, D //
	, E //
	, F //
	, G //
	, H //
	, I //
	, J //
	, K //
	, L //
	;

	public static Serie getSerieAtualPorData(LocalDate data) {
		LocalDate terceiraSegundaFeira = getTerceiraSegundaFeiraDesteMes(data);
		return getSerieAtualPorData(data, terceiraSegundaFeira);
	}

	static Serie getSerieAtualPorData(LocalDate data, LocalDate terceiraSegundaFeiraDesteMes) {
		// Se a data for anterior a 3a segunda-feira, a serie eh a do mes atual
		int posicao = data.getMonthOfYear();
		if (data.isBefore(terceiraSegundaFeiraDesteMes) || data.isEqual(terceiraSegundaFeiraDesteMes)) {
			posicao--;
		}
		return values()[posicao % 12];
	}

	public static Serie getProximaSeriePorData(LocalDate data) {
		Serie atual = getSerieAtualPorData(data);
		return values()[(atual.ordinal() + 1) % 12];
	}

	static LocalDate getTerceiraSegundaFeiraDesteMes(LocalDate data) {
		// Vai para alguma segunda-feira deste mes
		LocalDate segundaFeira = new LocalDate(data).withDayOfWeek(1);
		// Se foi para o ano ou mes anterior
		if (segundaFeira.getYear() < data.getYear() || segundaFeira.getMonthOfYear() < data.getMonthOfYear()) {
			segundaFeira = segundaFeira.plusWeeks(1);
		}
		// Se foi para o ano ou mes posterior
		if (segundaFeira.getYear() > data.getYear() || segundaFeira.getMonthOfYear() > data.getMonthOfYear()) {
			segundaFeira = segundaFeira.minusWeeks(1);
		}
		// Encontra a primeira segunda-feira deste mes
		while (segundaFeira.getDayOfMonth() > 7) {
			segundaFeira = segundaFeira.minusWeeks(1);
		}
		// Vai para a terceira segunda-feira deste mes
		LocalDate terceiraSegundaFeira = segundaFeira.plusWeeks(2);

		return terceiraSegundaFeira;
	}

	public LocalDate getDataVencimento(LocalDate hojeFicticio) {
		LocalDate terceiraSegundaFeira = getTerceiraSegundaFeiraDesteMes(hojeFicticio);
		Serie serieAtual = getSerieAtualPorData(hojeFicticio, terceiraSegundaFeira);
		if (this.ordinal() == serieAtual.ordinal()) {
			// se for a mesma Serie, o vencimento é a proxima 3a segunda
			if (hojeFicticio.isBefore(terceiraSegundaFeira) || hojeFicticio.equals(terceiraSegundaFeira))
				return terceiraSegundaFeira;
			// a 3a segunda deste mes ja passou, vamos para a do proximo mes
			return getTerceiraSegundaFeiraDesteMes(hojeFicticio.plusMonths(1));
		} else if (this.ordinal() == (serieAtual.ordinal() + 1) % 12) {
			// se for a próxima Serie, o vencimento é
			// a 3a segunda-feira do mes que vem, caso a
			// 3a segunda-feira deste mes ainda nao tenha
			// passado. Caso ja tenha passado, sera a 3a
			// segunda-feira daqui a 2 meses.
			int meses = (hojeFicticio.isAfter(terceiraSegundaFeira)) ? 2 : 1;
			return getTerceiraSegundaFeiraDesteMes(hojeFicticio.plusMonths(meses));
		}
		throw new IllegalArgumentException(
				"Serie nao vencera no mes atual ou no proximo para a data "
						+ hojeFicticio);
	}

	public String getDescricao() {
		return "Série " + name();
	}

	private final Pattern seriePattern = Pattern.compile("\\w{4}([A-L])\\d+");

	public boolean isSerieDaOpcao(String codigoNegociacao) {
		if (codigoNegociacao == null || codigoNegociacao.length() < 6)
			return false;
		Matcher matcher = seriePattern.matcher(codigoNegociacao);
		if (matcher == null || matcher.matches() == false)
			return false;
		return name().equals(matcher.group(1));
	}

	public static Map<Serie, LocalDate> getVencimentosPorPeriodo(LocalDate inicio,
			LocalDate fim) {

		Map<Serie, LocalDate> vencimentos = new HashMap<Serie, LocalDate>();
		while (inicio.isBefore(fim)) {
			Serie serieAtual = getSerieAtualPorData(inicio);
			LocalDate vencimento = serieAtual.getDataVencimento(inicio);
			if (vencimento.isAfter(fim))
				break;
			vencimentos.put(serieAtual, vencimento);
			inicio = inicio.plusMonths(1).withDayOfMonth(1);
		}
		return vencimentos;
	}
}

package br.eti.ranieri.opcoesweb.importacao.offline;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.joda.time.LocalDate;

public class CotacaoBDI implements Comparable<CotacaoBDI> {

	// Para todos os ativos
	private LocalDate dataPregao;
	private CodigoBDI codigoBdi;
	private TipoMercadoBDI tipoMercado;
	private String codigoNegociacao;
	private long fechamento;
	private long volume;
	private long oscilacao;
	// Para opções
	private long precoExercicio;
	private long dataVencimento;

	public CotacaoBDI() {
	}

	public CotacaoBDI(LocalDate dataPregao, CodigoBDI codigoBdi, TipoMercadoBDI tipoMercado, String codigoNegociacao, long fechamento, long volume, long oscilacao, long precoExercicio,
			long dataVencimento) {

		this.dataPregao = dataPregao;
		this.codigoBdi = codigoBdi;
		this.tipoMercado = tipoMercado;
		this.codigoNegociacao = codigoNegociacao;
		this.fechamento = fechamento;
		this.volume = volume;
		this.oscilacao = oscilacao;
		this.precoExercicio = precoExercicio;
		this.dataVencimento = dataVencimento;
	}

	public LocalDate getDataPregao() {
		return dataPregao;
	}

	public void setDataPregao(LocalDate dataPregao) {
		this.dataPregao = dataPregao;
	}

	public CodigoBDI getCodigoBdi() {
		return codigoBdi;
	}

	public void setCodigoBdi(CodigoBDI codigoBdi) {
		this.codigoBdi = codigoBdi;
	}

	public TipoMercadoBDI getTipoMercado() {
		return tipoMercado;
	}

	public void setTipoMercado(TipoMercadoBDI tipoMercado) {
		this.tipoMercado = tipoMercado;
	}

	public String getCodigoNegociacao() {
		return codigoNegociacao;
	}

	public void setCodigoNegociacao(String codigoNegociacao) {
		this.codigoNegociacao = codigoNegociacao;
	}

	public long getFechamento() {
		return fechamento;
	}

	public void setFechamento(long fechamento) {
		this.fechamento = fechamento;
	}

	public long getVolume() {
		return volume;
	}

	public void setVolume(long volume) {
		this.volume = volume;
	}

	public long getOscilacao() {
		return oscilacao;
	}

	public void setOscilacao(long oscilacao) {
		this.oscilacao = oscilacao;
	}

	public long getPrecoExercicio() {
		return precoExercicio;
	}

	public void setPrecoExercicio(long precoExercicio) {
		this.precoExercicio = precoExercicio;
	}

	public long getDataVencimento() {
		return dataVencimento;
	}

	public LocalDate getLocalDateVencimento() {
		int dia = (int) dataVencimento % 100;
		int mes = (int) (dataVencimento / 100) % 100;
		int ano = (int) (dataVencimento / 10000) % 10000;
		if (ano < 100)
			ano += (ano < 50) ? 2000 : 1900;
		return new LocalDate(ano, mes, dia);
	}

	public void setDataVencimento(long dataVencimento) {
		this.dataVencimento = dataVencimento;
	}

	public int compareTo(CotacaoBDI o) {
		return new CompareToBuilder() //
				.append(this.dataPregao, o.dataPregao) //
				.append(this.codigoBdi, o.codigoBdi) //
				.append(this.tipoMercado, o.tipoMercado) //
				.append(this.codigoNegociacao, o.codigoNegociacao) //
				.toComparison();
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this) //
				.append("dataPregao", this.dataPregao) //
				.append("codigoNegociacao", this.codigoNegociacao) //
				.append("fechamento", this.fechamento) //
				.append("exercicio", this.precoExercicio) //
				.toString();
	}
}

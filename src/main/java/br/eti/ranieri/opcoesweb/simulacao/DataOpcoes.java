package br.eti.ranieri.opcoesweb.simulacao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

import br.eti.ranieri.opcoesweb.estado.CotacaoOpcao;
import br.eti.ranieri.opcoesweb.estado.Variavel;
import br.eti.ranieri.opcoesweb.format.FormatadorNumerico;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

public class DataOpcoes implements Serializable {

    private LocalDate data;
    private List<CodigoVariaveis> codigosVariaveis;

    public DataOpcoes(final double precoAcao, LocalDate data, List<CotacaoOpcao> opcoes) {
	this.data = data;
	this.codigosVariaveis = new ArrayList<CodigoVariaveis>(
		Collections2.transform(opcoes, new Function<CotacaoOpcao, CodigoVariaveis>() {
		    public CodigoVariaveis apply(CotacaoOpcao opcao) {
			return new CodigoVariaveis(DataOpcoes.this.data, precoAcao, opcao);
		    }
		}));
    }

    public LocalDate getData() {
	return data;
    }

    public List<CodigoVariaveis> getCodigosVariaveis() {
	return codigosVariaveis;
    }

    public class CodigoVariaveis implements Serializable {

	public final LocalDate data;
	public final String precoAcao;
	public final String codigo;
	public final String precoReal;
	public final String precoTeorico;
	public final String volume;
	public final String strike;
	public final String taxaVE;
	public final String atm;
	public final String bosi;
	public final String naoVende;
	public final String theVE;
	public final String vdx;

	public CodigoVariaveis(LocalDate data, double precoAcao, CotacaoOpcao opcao) {
	    this.data = data;
	    this.precoAcao = FormatadorNumerico.FORMATADOR_DINHEIRO.formatar(precoAcao);
	    codigo = opcao.getCodigo();
	    precoReal = getVariavel(opcao, Variavel.PRECO_REAL);
	    precoTeorico = getVariavel(opcao, Variavel.PRECO_TEORICO);
	    volume = getVariavel(opcao, Variavel.VOLUME);
	    strike = getVariavel(opcao, Variavel.STRIKE);
	    taxaVE = getVariavel(opcao, Variavel.TAXA_VE);
	    atm = getVariavel(opcao, Variavel.ATM);
	    bosi = getVariavel(opcao, Variavel.BOSI);
	    naoVende = getVariavel(opcao, Variavel.NAO_VENDE);
	    theVE = getVariavel(opcao, Variavel.THE_VE);
	    vdx = getVariavel(opcao, Variavel.VDX);
	}

	private String getVariavel(CotacaoOpcao opcao, Variavel variavel) {
	    if (opcao.getVariaveis() == null)
		return "N/A";
	    return variavel.getFormatador().formatar(opcao.getVariaveis().get(variavel));
	}
    }
}

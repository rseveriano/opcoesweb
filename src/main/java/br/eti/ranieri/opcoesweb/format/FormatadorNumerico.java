package br.eti.ranieri.opcoesweb.format;

import java.text.Format;
import java.text.MessageFormat;
import java.util.Locale;

public class FormatadorNumerico {

    private static final Locale localeBR = new Locale("pt", "BR");
    public static final FormatadorNumerico FORMATADOR_DINHEIRO = new FormatadorNumerico(new MessageFormat("{0,number,currency}", localeBR));
    public static final FormatadorNumerico FORMATADOR_DECIMAL = new FormatadorNumerico(new MessageFormat("{0,number,#,##0.00}", localeBR));
    public static final FormatadorNumerico FORMATADOR_PORCENTAGEM = new FormatadorNumerico(new MessageFormat("{0,number,#,##0.0}%", localeBR), 100.0);
    public static final FormatadorNumerico FORMATADOR_PORMILHAGEM = new FormatadorNumerico(new MessageFormat("{0,number,#,##0.00}%", localeBR), 100.0);
    public static final FormatadorNumerico FORMATADOR_MILHAR = new FormatadorNumerico(new MessageFormat("{0,number,#,##0}k", localeBR), 0.001);
    public static final FormatadorNumerico FORMATADOR_PADRAO = new FormatadorNumerico(new MessageFormat("{0}"));
//    private static final Map<TipoFormatacaoNumerica, FormatadorNumerico> mapaPorTipo = new HashMap<TipoFormatacaoNumerica, FormatadorNumerico>();
    

//    static {
//        mapaPorTipo.put(TipoFormatacaoNumerica.DINHEIRO, new FormatadorNumerico(new MessageFormat("{0,number,currency}", localeBR)));
//        mapaPorTipo.put(TipoFormatacaoNumerica.DECIMAL, new FormatadorNumerico(new MessageFormat("{0,number,#,##0.00}", localeBR)));
//        mapaPorTipo.put(TipoFormatacaoNumerica.PORCENTAGEM, new FormatadorNumerico(new MessageFormat("{0,number,#,##0.0}%", localeBR), 100.0));
//        mapaPorTipo.put(TipoFormatacaoNumerica.PORMILHAR, new FormatadorNumerico(new MessageFormat("{0,number,#,##0.00}%", localeBR), 100.0));
//        mapaPorTipo.put(TipoFormatacaoNumerica.INTEIRO, new FormatadorNumerico(new MessageFormat("{0,number,#,##0}", localeBR)));
//    }

//    public static FormatadorNumerico obterPor(TipoFormatacaoNumerica tipo) {
//        FormatadorNumerico formatador = mapaPorTipo.get(tipo);
//        if (formatador != null) {
//            return formatador;
//        }
//        return FORMATADOR_PADRAO;
//    }

    public static String formatarDinheiro(Object valor) {
//        return obterPor(TipoFormatacaoNumerica.DINHEIRO).formatar(valor);
    	return FORMATADOR_DINHEIRO.formatar(valor);
    }

    public static String formatarPorcentagem(Object valor) {
//        return obterPor(TipoFormatacaoNumerica.PORCENTAGEM).formatar(valor);
    	return FORMATADOR_PORCENTAGEM.formatar(valor);
    }

    private Format format;
    private Double fator;

    private FormatadorNumerico(Format format) {
        this(format, null);
    }

    private FormatadorNumerico(Format format, Double fator) {
        this.format = format;
        this.fator = fator;
    }

	public String formatar(Object valor) {
        if (fator == null) {
       		return format.format(new Object[]{valor});
        }
        return format.format(new Object[]{((Number) valor).doubleValue() * fator});
    }
}

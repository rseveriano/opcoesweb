package br.eti.ranieri.opcoesweb.importacao.online;

import java.util.ArrayList;
import java.util.List;

public class BanifXmlResultados {
	private final List<BanifXmlPapel> papeis = new ArrayList<BanifXmlPapel>();

	public void addPapel(BanifXmlPapel papel) {
		papeis.add(papel);
	}

	public List<BanifXmlPapel> getPapeis() {
		return papeis;
	}
}

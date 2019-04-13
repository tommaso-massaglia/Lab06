package it.polito.tdp.meteo;

import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polito.tdp.meteo.bean.Citta;
import it.polito.tdp.meteo.bean.Rilevamento;
import it.polito.tdp.meteo.bean.SimpleCity;
import it.polito.tdp.meteo.db.MeteoDAO;

public class Model {

	private final static int COST = 100;
	private final static int NUMERO_GIORNI_CITTA_CONSECUTIVI_MIN = 3;
	private final static int NUMERO_GIORNI_CITTA_MAX = 6;
	private final static int NUMERO_GIORNI_TOTALI = 15;

	public Map<String, Citta> listacitta;

	public Model() {
		this.listacitta = new HashMap<String, Citta>();
		this.popolaListaCitta();
	}

	public String getUmiditaMedia(int mese) {
		int somma = 0;
		int contatore = 0;
		String risultato = new String();
		MeteoDAO dao = new MeteoDAO();
		for (Citta c : listacitta.values()) {
			for (Rilevamento r : dao.getAllRilevamentiLocalitaMese(mese, c.getNome())) {
				somma+=r.getUmidita();
				contatore++;
			}
			risultato+=c.getNome()+" ha avuto nel mese di "+Month.of(mese)+" un'umidità media di: "+somma/contatore+"\n";
		}
		return risultato;
	}

	public String trovaSequenza(int mese) {

		return "TODO!";
	}

	private Double punteggioSoluzione(List<SimpleCity> soluzioneCandidata) {

		double score = 0.0;
		return score;
	}

	private boolean controllaParziale(List<SimpleCity> parziale) {

		return true;
	}

	private void popolaListaCitta() {
		MeteoDAO dao = new MeteoDAO();
		for (Rilevamento r : dao.getAllRilevamenti()) {
			if (!this.listacitta.containsKey(r.getLocalita())) {
				this.listacitta.put(r.getLocalita(), new Citta(r.getLocalita(), r));
				System.out.println(listacitta.get(r.getLocalita()));
			} else {
				this.listacitta.get(r.getLocalita()).addRilevamenti(r);
			}
		}
	}

}

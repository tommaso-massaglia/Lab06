package it.polito.tdp.meteo;

import java.time.Month;
import java.util.ArrayList;
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

	private Map<String, Citta> listacitta;
	private double best_score;
	private List<SimpleCity> best_list;
	private MeteoDAO dao;

	public Model() {
		this.listacitta = new HashMap<String, Citta>();
		this.popolaListaCitta();
		this.best_list = new ArrayList<SimpleCity>();
		this.dao = new MeteoDAO();
	}

	public String getUmiditaMedia(int mese) {
		int somma = 0;
		int contatore = 0;
		String risultato = new String();
		MeteoDAO dao = new MeteoDAO();
		for (Citta c : listacitta.values()) {
			for (Rilevamento r : dao.getAllRilevamentiLocalitaMese(mese, c.getNome())) {
				somma += r.getUmidita();
				contatore++;
			}
			risultato += c.getNome() + " ha avuto nel mese di " + Month.of(mese) + " un'umidità media di: "
					+ somma / contatore + "\n";
		}
		return risultato;
	}

	public String trovaSequenza(int mese) {

		best_score = 999999999.0;
		List<SimpleCity> parziale = new ArrayList<SimpleCity>();
		this.cerca(mese, parziale, 0);
		String result = new String();
		int contatore = 1;
		
		System.out.print(best_list);
		
		for (SimpleCity sc : best_list) {
			result += contatore + ". " + sc.toString()+"\n";
			contatore++;
		}
		result += this.best_score;
		
		return result;
	}

	public void cerca(int mese, List<SimpleCity> parziale, int L) {
		// Casi Terminali

		if (!this.controllaParziale(parziale)) {
			System.err.println("FERMATO PER PARZIALE NON VALIDO");
			return;
		}
		else if (this.punteggioSoluzione(parziale) > this.best_score) {
			System.err.println("FERMATO PER SCORE MAGGIORE");
			return;
		}
		if (L == Model.NUMERO_GIORNI_TOTALI && this.punteggioSoluzione(parziale) < this.best_score) {
			this.best_list = new ArrayList<SimpleCity>(parziale);
			this.best_score = this.punteggioSoluzione(best_list);
			System.out.println(L);
			System.out.println(this.best_score);
			System.out.println(this.best_list);
			return;
		}
		// Iterazioni
		for (Rilevamento r : dao.getAllRilevamentiMeseGG(mese, L + 1)) {
			parziale.add(new SimpleCity(r.getLocalita(), r.getUmidita()));
			this.cerca(mese, parziale, L + 1);
			parziale.remove(L);
		}

	}

	private Double punteggioSoluzione(List<SimpleCity> soluzioneCandidata) {
		double score = 0.0;
		for (int i = 0; i < soluzioneCandidata.size(); i++) {
			if (i != 0 && !soluzioneCandidata.get(i).equals(soluzioneCandidata.get(i - 1))) {
				score += Model.COST + soluzioneCandidata.get(i).getCosto();
			} else {
				score += soluzioneCandidata.get(i).getCosto();
			}
		}
		return score;
	}

	private boolean controllaParziale(List<SimpleCity> parziale) {
		int contatore = 0;
		SimpleCity precedente = null;
		for (SimpleCity sc : parziale) {
			if (sc.equals(precedente)) {
				contatore++;
				if (contatore > Model.NUMERO_GIORNI_CITTA_MAX) {
					return false;
				}

			} else {
				if (contatore > 0 && contatore < Model.NUMERO_GIORNI_CITTA_CONSECUTIVI_MIN) {
					return false;
				}
				precedente = sc;
				contatore = 1;
			}
		}
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

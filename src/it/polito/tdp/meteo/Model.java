package it.polito.tdp.meteo;

import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	private List<Rilevamento> rilevamenti;
	private double best_score;
	private List<SimpleCity> best_list;
	//private MeteoDAO dao;

	public Model() {
		this.listacitta = new HashMap<String, Citta>();
		this.rilevamenti = new ArrayList<Rilevamento>();
		this.popolaListaCitta();
		//this.best_list = new ArrayList<SimpleCity>();
	}

	/**
	 * Restituisce l'umidità media del mese passato come parametro
	 * @param mese un {@link int} del mese
	 * @return una stringa contenente l'umidità media
	 */
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

	/**
	 * Riceve come parametro un {@link int} del mese e restituisce la sequenza
	 * ottima in cui l'impiegato dovrebbe visitare le città tenenendo conto dei
	 * parametri specificati dagli {@link static} sopra; il metodo è ricorsivo,
	 * salva i dati prima di iniziare a passarli in rassegna ed esce se il punteggio
	 * è superiore al migliore salvato o se non rispetta i parametri
	 * @param mese un {@link int} che indica il mese
	 * @return una stringa contenente riga per riga la sequenza ottima
	 */
	public String trovaSequenza(int mese) {
		
		double ti = System.currentTimeMillis();

		best_score = 999999999.0;
		this.best_list = new ArrayList<SimpleCity>();
		List<SimpleCity> parziale = new ArrayList<SimpleCity>();
		this.cerca(mese, parziale, 0);
		String result = new String();
		int contatore = 1;
		
		System.out.print(best_list);
		
		for (SimpleCity sc : best_list) {
			result += contatore + ". " + sc.toString()+"\n";
			contatore++;
		}
		result += "Punteggio: "+this.best_score+", Tempo Impiegato: "+(System.currentTimeMillis()-ti)/1000+" secondi";
		
		return result;
	}

	/**
	 * La funzione che cerca la soluzione ottima
	 * @param mese il mese di cui cercare la sequenza
	 * @param parziale la lista temporanea trovata
	 * @param L il livello in cui iniziare il metodo
	 */
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
		for (Rilevamento r : this.getRilevamentiGG(mese, L + 1)) {
			parziale.add(new SimpleCity(r.getLocalita(), r.getUmidita()));
			this.cerca(mese, parziale, L + 1);
			parziale.remove(L);
		}

	}

	/**
	 * calcola il punteggio del parziale passato
	 * @param soluzioneCandidata una lista contenente una soluzione parziale del
	 * problema
	 * @return il punteggio
	 */
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

	/**
	 * controlla che la lista rispetti i parametri {@link static} decisi prima
	 * @param parziale la lista da controllare
	 * @return {@link true} se corretto e {@link false} altrimenti
	 */
	private boolean controllaParziale(List<SimpleCity> parziale) {
		int contatore = 0;
		Set<String> cittavisitate = new HashSet<String>();
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
				cittavisitate.add(sc.getNome());
			}
		}
		if (cittavisitate.size()!=this.listacitta.size() && parziale.size()==Model.NUMERO_GIORNI_TOTALI)
			return false;
		return true;
	}
	
	// Ottiene tutti i rilevamenti del giorno specificato nel mese specificato
	@SuppressWarnings("deprecation")
	private List<Rilevamento> getRilevamentiGG(int mese,int giorno){
		List<Rilevamento> risultato = new ArrayList<Rilevamento>();
		for (Rilevamento r : this.rilevamenti) {
			if (r.getData().getMonth()==mese-1 && r.getData().getDate()==giorno) {
				risultato.add(r);
			}
		}
		return risultato;
	}

	// Popola la lista città e rilevamenti dal database
	private void popolaListaCitta() {
		MeteoDAO dao = new MeteoDAO();
		for (Rilevamento r : dao.getAllRilevamenti()) {
			this.rilevamenti.add(r);
			if (!this.listacitta.containsKey(r.getLocalita())) {
				this.listacitta.put(r.getLocalita(), new Citta(r.getLocalita(), r));
				System.out.println(listacitta.get(r.getLocalita()));
			} else {
				this.listacitta.get(r.getLocalita()).addRilevamenti(r);
			}
		}
	}

}

package cat.copernic.Projecte1_Equip5.logic;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Prefixos telefònics internacionals (codis de país de l'UIT) de tots els
 * països i territoris. És la font única: el desplegable del registre
 * ({@code client/alta.html}) en pinta les opcions i la validació del servidor
 * ({@link UsuariLogic#isPrefixTelefonValid}) comprova que el valor enviat hi
 * sigui, així no es pot inventar un prefix a mà a la petició.
 *
 * Els països que comparteixen codi (p. ex. el +1 d'Amèrica del Nord i el
 * Carib, o el +7 de Rússia i Kazakhstan) apareixen cadascun amb el seu nom,
 * però el valor que viatja al servidor és només el codi ("+1"). El nom del país
 * surt en català gràcies a les dades de {@link Locale} del JDK.
 */
public final class PrefixosTelefon {

    /**
     * Una opció del desplegable.
     *
     * @param codi prefix amb el + (ex. "+34"); és el valor que s'envia
     * @param nom nom del país en català (ex. "Espanya")
     * @param primer true si és el primer país de la llista amb aquest codi (es
     * fa servir per marcar només una opció com a seleccionada)
     */
    public record Prefix(String codi, String nom, boolean primer) {

        /**
         * Text que veu l'usuari al desplegable.
         *
         * @return per exemple "+34 · Espanya"
         */
        public String etiqueta() {
            return codi + " · " + nom;
        }
    }

    /**
     * Prefix que es preselecciona si l'usuari encara no n'ha triat cap.
     */
    public static final String PER_DEFECTE = "+34";

    // Parelles "codi ISO del país:prefix sense el +". El país és el que dóna el nom.
    private static final String[] PAISOS = {
        "AF:93", "AL:355", "DZ:213", "AS:1", "AD:376", "AO:244", "AI:1", "AG:1", "AR:54", "AM:374",
        "AW:297", "AU:61", "AT:43", "AZ:994", "BS:1", "BH:973", "BD:880", "BB:1", "BY:375", "BE:32",
        "BZ:501", "BJ:229", "BM:1", "BT:975", "BO:591", "BQ:599", "BA:387", "BW:267", "BR:55", "IO:246",
        "VG:1", "BN:673", "BG:359", "BF:226", "BI:257", "KH:855", "CM:237", "CA:1", "CV:238", "KY:1",
        "CF:236", "TD:235", "CL:56", "CN:86", "CX:61", "CC:61", "CO:57", "KM:269", "CG:242", "CD:243",
        "CK:682", "CR:506", "CI:225", "HR:385", "CU:53", "CW:599", "CY:357", "CZ:420", "DK:45", "DJ:253",
        "DM:1", "DO:1", "EC:593", "EG:20", "SV:503", "GQ:240", "ER:291", "EE:372", "SZ:268", "ET:251",
        "FK:500", "FO:298", "FJ:679", "FI:358", "FR:33", "GF:594", "PF:689", "GA:241", "GM:220", "GE:995",
        "DE:49", "GH:233", "GI:350", "GR:30", "GL:299", "GD:1", "GP:590", "GU:1", "GT:502", "GG:44",
        "GN:224", "GW:245", "GY:592", "HT:509", "HN:504", "HK:852", "HU:36", "IS:354", "IN:91", "ID:62",
        "IR:98", "IQ:964", "IE:353", "IM:44", "IL:972", "IT:39", "JM:1", "JP:81", "JE:44", "JO:962",
        "KZ:7", "KE:254", "KI:686", "XK:383", "KW:965", "KG:996", "LA:856", "LV:371", "LB:961", "LS:266",
        "LR:231", "LY:218", "LI:423", "LT:370", "LU:352", "MO:853", "MG:261", "MW:265", "MY:60", "MV:960",
        "ML:223", "MT:356", "MH:692", "MQ:596", "MR:222", "MU:230", "YT:262", "MX:52", "FM:691", "MD:373",
        "MC:377", "MN:976", "ME:382", "MS:1", "MA:212", "MZ:258", "MM:95", "NA:264", "NR:674", "NP:977",
        "NL:31", "NC:687", "NZ:64", "NI:505", "NE:227", "NG:234", "NU:683", "NF:672", "KP:850", "MK:389",
        "MP:1", "NO:47", "OM:968", "PK:92", "PW:680", "PS:970", "PA:507", "PG:675", "PY:595", "PE:51",
        "PH:63", "PL:48", "PT:351", "PR:1", "QA:974", "RE:262", "RO:40", "RU:7", "RW:250", "BL:590",
        "SH:290", "KN:1", "LC:1", "MF:590", "PM:508", "VC:1", "WS:685", "SM:378", "ST:239", "SA:966",
        "SN:221", "RS:381", "SC:248", "SL:232", "SG:65", "SX:1", "SK:421", "SI:386", "SB:677", "SO:252",
        "ZA:27", "KR:82", "SS:211", "ES:34", "LK:94", "SD:249", "SR:597", "SJ:47", "SE:46", "CH:41",
        "SY:963", "TW:886", "TJ:992", "TZ:255", "TH:66", "TL:670", "TG:228", "TK:690", "TO:676", "TT:1",
        "TN:216", "TR:90", "TM:993", "TC:1", "TV:688", "UG:256", "UA:380", "AE:971", "GB:44", "US:1",
        "UY:598", "VI:1", "UZ:998", "VU:678", "VA:39", "VE:58", "VN:84", "WF:681", "EH:212", "YE:967",
        "ZM:260", "ZW:263", "AX:358"
    };

    private static final List<Prefix> LLISTA = construirLlista();
    private static final Set<String> CODIS = construirCodis(LLISTA);

    private PrefixosTelefon() {
    }

    /**
     * Tots els prefixos, ordenats pel nom del país en català, amb Espanya al
     * principi.
     *
     * @return llista immutable d'opcions per al desplegable
     */
    public static List<Prefix> llista() {
        return LLISTA;
    }

    /**
     * Comprova que un prefix és un dels de la llista.
     *
     * @param prefix prefix amb el + (ex. "+34"); pot ser null
     * @return true si existeix
     */
    public static boolean esValid(String prefix) {
        return prefix != null && CODIS.contains(prefix.trim());
    }

    private static List<Prefix> construirLlista() {
        Locale catala = Locale.forLanguageTag("ca");
        record Pais(String nom, String codi, boolean espanya) {

        }
        List<Pais> paisos = new ArrayList<>();
        for (String parella : PAISOS) {
            String[] parts = parella.split(":");
            String nom = new Locale.Builder().setRegion(parts[0]).build().getDisplayCountry(catala);
            if (nom == null || nom.isBlank()) {
                nom = parts[0]; // si el JDK no coneix el país, es mostra el codi ISO
            }
            paisos.add(new Pais(nom, "+" + parts[1], "ES".equals(parts[0])));
        }
        Collator collator = Collator.getInstance(catala);
        paisos.sort(Comparator.comparing((Pais p) -> !p.espanya())
                .thenComparing(Pais::nom, collator));

        List<Prefix> resultat = new ArrayList<>();
        Set<String> jaVists = new LinkedHashSet<>();
        for (Pais p : paisos) {
            resultat.add(new Prefix(p.codi(), p.nom(), jaVists.add(p.codi())));
        }
        return List.copyOf(resultat);
    }

    private static Set<String> construirCodis(List<Prefix> llista) {
        Set<String> codis = new LinkedHashSet<>();
        for (Prefix p : llista) {
            codis.add(p.codi());
        }
        return Set.copyOf(codis);
    }
}

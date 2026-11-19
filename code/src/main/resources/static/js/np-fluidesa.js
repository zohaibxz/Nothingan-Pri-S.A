/**
 * Nothingan Pri - comportament comú de la interfície.
 *
 * RN45 (Fluïdesa): quan s'envia un formulari l'aplicació pot trigar (pujada de
 * fitxers, desar a la base de dades...). Mentre dura, l'usuari veu que la
 * pàgina està treballant: el botó canvia de text i, si triga més de 0,4 s,
 * apareix un avís. Així la pantalla mai sembla "congelada" sense explicació.
 *
 * RN46 (Amigable i intuïtiu):
 *  - Un formulari no es pot enviar dues vegades (evita reserves, lliuraments
 *    o devolucions duplicats per un doble clic).
 *  - Dates coherents: un camp de data amb l'atribut data-despres-de="idAltreCamp"
 *    no deixa triar una data igual o anterior a la de l'altre camp.
 *
 * Atributs opcionals als formularis:
 *  - data-ocupat="Text": text del botó i de l'avís mentre s'envia.
 *  - data-sense-ocupat: no aplica aquest comportament al formulari.
 */
(function () {
    'use strict';

    var RETARD_AVIS_MS = 400;
    var TEXT_BOTO_DEFECTE = 'Processant…';
    var TEXT_AVIS_DEFECTE = 'Processant la sol·licitud…';
    var TEXT_AVIS_FITXERS = 'Pujant fitxers… pot trigar uns segons. No tanquis la pàgina.';

    var temporitzadorAvis = null;
    var botonsModificats = [];

    /** Afegeix els estils de l'avís una sola vegada. */
    function injectarEstils() {
        if (document.getElementById('np-ocupat-estils')) {
            return;
        }
        var estil = document.createElement('style');
        estil.id = 'np-ocupat-estils';
        estil.textContent =
                '#np-ocupat{position:fixed;left:50%;bottom:24px;transform:translateX(-50%);' +
                'z-index:2147483647;max-width:90vw;padding:10px 18px;border-radius:10px;' +
                'background:#1b2433;color:#fff;font:600 14px/1.4 "Segoe UI",system-ui,sans-serif;' +
                'box-shadow:0 6px 20px rgba(0,0,0,.25)}' +
                '#np-ocupat[hidden]{display:none}' +
                'button[aria-disabled="true"],input[aria-disabled="true"]{cursor:progress}';
        document.head.appendChild(estil);
    }

    /** Retorna l'element de l'avís, creant-lo si encara no existeix. */
    function elementAvis() {
        var avis = document.getElementById('np-ocupat');
        if (!avis) {
            injectarEstils();
            avis = document.createElement('div');
            avis.id = 'np-ocupat';
            avis.setAttribute('role', 'status');
            avis.setAttribute('aria-live', 'polite');
            avis.hidden = true;
            document.body.appendChild(avis);
        }
        return avis;
    }

    /**
     * Activa o desactiva l'estat "treballant": cursor d'espera, aria-busy i,
     * si l'operació triga, un avís amb el missatge indicat.
     *
     * @param {boolean} actiu true per començar, false per acabar
     * @param {string} [missatge] text de l'avís
     */
    function ocupat(actiu, missatge) {
        clearTimeout(temporitzadorAvis);
        var avis = elementAvis();
        if (actiu) {
            document.documentElement.style.cursor = 'progress';
            document.body.setAttribute('aria-busy', 'true');
            temporitzadorAvis = setTimeout(function () {
                avis.textContent = missatge || TEXT_AVIS_DEFECTE;
                avis.hidden = false;
            }, RETARD_AVIS_MS);
        } else {
            document.documentElement.style.cursor = '';
            document.body.removeAttribute('aria-busy');
            avis.hidden = true;
        }
    }

    /** Indica si un element és un botó que envia el formulari. */
    function esBotoEnviar(el) {
        return (el.tagName === 'BUTTON' || el.tagName === 'INPUT') && el.type === 'submit';
    }

    /** Indica si el formulari porta algun fitxer seleccionat. */
    function portaFitxers(form) {
        var inputs = form.querySelectorAll('input[type="file"]');
        for (var i = 0; i < inputs.length; i++) {
            if (inputs[i].files && inputs[i].files.length > 0) {
                return true;
            }
        }
        return false;
    }

    /** Deixa els botons com estaven (si l'usuari torna enrere amb el navegador). */
    function restaurarBotons() {
        botonsModificats.forEach(function (b) {
            b.element.disabled = false;
            b.element.removeAttribute('aria-disabled');
            if (b.html !== null) {
                b.element.innerHTML = b.html;
            }
        });
        botonsModificats = [];
        document.querySelectorAll('form[data-enviant]').forEach(function (f) {
            f.removeAttribute('data-enviant');
        });
        ocupat(false);
    }

    /** Bloqueja el doble enviament i informa que l'operació està en marxa. */
    document.addEventListener('submit', function (e) {
        var form = e.target;
        if (e.defaultPrevented || !(form instanceof HTMLFormElement)
                || form.hasAttribute('data-sense-ocupat')
                || (form.target && form.target !== '_self')) {
            return; // p. ex. un confirm() cancel·lat: no es marca com a enviant
        }
        if (form.hasAttribute('data-enviant')) {
            e.preventDefault();
            return;
        }
        form.setAttribute('data-enviant', 'true');

        var textBoto = form.getAttribute('data-ocupat') || TEXT_BOTO_DEFECTE;
        var textAvis = form.getAttribute('data-ocupat')
                || (portaFitxers(form) ? TEXT_AVIS_FITXERS : TEXT_AVIS_DEFECTE);
        var enviador = e.submitter || null;
        var botons = Array.prototype.filter.call(form.elements, esBotoEnviar);

        // Es desactiva després que el navegador hagi recollit les dades;
        // si es fes ara, el botó premut no enviaria el seu valor.
        setTimeout(function () {
            botons.forEach(function (boto) {
                if (boto.disabled) {
                    return;
                }
                var guardat = {element: boto, html: null};
                if ((boto === enviador || (!enviador && boto === botons[0]))
                        && boto.tagName === 'BUTTON') {
                    guardat.html = boto.innerHTML;
                    boto.textContent = textBoto;
                }
                boto.disabled = true;
                boto.setAttribute('aria-disabled', 'true');
                botonsModificats.push(guardat);
            });
        }, 0);

        ocupat(true, textAvis);
    });

    window.addEventListener('pageshow', function (e) {
        if (e.persisted) {
            restaurarBotons();
        }
    });

    /**
     * Dates coherents: el camp amb data-despres-de="id" només accepta dates
     * posteriors a les de l'altre camp (mínim: el dia següent).
     */
    function enllacarDates() {
        document.querySelectorAll('input[type="date"][data-despres-de]').forEach(function (fi) {
            var inici = document.getElementById(fi.getAttribute('data-despres-de'));
            if (!inici) {
                return;
            }
            var minimOriginal = fi.getAttribute('min');

            function dataMes1Dia(valor) {
                var d = new Date(valor + 'T00:00:00');
                d.setDate(d.getDate() + 1);
                return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0')
                        + '-' + String(d.getDate()).padStart(2, '0');
            }

            function actualitzar() {
                if (!inici.value) {
                    if (minimOriginal) {
                        fi.min = minimOriginal;
                    }
                    return;
                }
                var minim = dataMes1Dia(inici.value);
                fi.min = minim;
                if (fi.value && fi.value < minim) {
                    fi.value = ''; // data incoherent: es buida perquè en triïn una de vàlida
                }
            }

            inici.addEventListener('change', actualitzar);
            actualitzar();
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', enllacarDates);
    } else {
        enllacarDates();
    }

    window.NP = {ocupat: ocupat};
})();

package cat.copernic.Projecte1_Equip5.logic;

import cat.copernic.Projecte1_Equip5.document.VehicleArxiu;
import cat.copernic.Projecte1_Equip5.entity.Enums;
import cat.copernic.Projecte1_Equip5.entity.Vehicle;
import cat.copernic.Projecte1_Equip5.repository.ReservaRepository;
import cat.copernic.Projecte1_Equip5.repository.VehicleArxiuRepository;
import cat.copernic.Projecte1_Equip5.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RN22 - Proves unitàries de la lògica de negoci de vehicles (JUnit 5 + Mockito).
 * Els repositoris són mocks: no cal cap base de dades.
 */
@ExtendWith(MockitoExtension.class)
class VehicleLogicTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private VehicleArxiuRepository arxiuRepository;

    @InjectMocks
    private VehicleLogic vehicleLogic;

    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1', '.', '4'};
    private static final int CINC_MB = 5 * 1024 * 1024;

    // ---------- Utilitats ----------
    private Vehicle vehicleValid() {
        return new Vehicle("1234BCD", "Seat", Enums.CategoriaVehicle.Turisme, "Ibiza",
                2020, 12.5, LocalDate.now().minusDays(1), Enums.Combustible.Gasolina,
                110, "Blau", 5, Enums.Transmissio.Manual, 200, 30,
                new BigDecimal("300.00"), "Sense comentaris");
    }

    private Vehicle vehicleAmb(String matricula, Enums.EstatVehicle estat) {
        Vehicle v = vehicleValid();
        v.setMatricula(matricula);
        v.setEstat(estat);
        return v;
    }

    private void llogadesAvui(String... matricules) {
        when(reservaRepository.matriculesLlogades(
                eq(Enums.EstatReserva.Lliurada), eq(Enums.EstatReserva.Activa), any(LocalDate.class)))
                .thenReturn(List.of(matricules));
    }

    private MockMultipartFile fitxer(String nom, String tipus, byte[] dades) {
        return new MockMultipartFile("fitxer", nom, tipus, dades);
    }

    private MultipartFile[] fotosJpg(int n) {
        MultipartFile[] r = new MultipartFile[n];
        for (int i = 0; i < n; i++) {
            r[i] = fitxer("foto" + i + ".jpg", "image/jpeg", JPG);
        }
        return r;
    }

    private byte[] ambCapcalera(byte[] capcalera, int mida) {
        byte[] b = new byte[mida];
        System.arraycopy(capcalera, 0, b, 0, capcalera.length);
        return b;
    }

    private void assertSaveError(Vehicle v, String fragment) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> vehicleLogic.saveVehicle(v));
        assertTrue(e.getMessage().contains(fragment),
                "Missatge inesperat: " + e.getMessage());
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    // =====================================================================
    // saveVehicle: alta / modificació amb validació
    // =====================================================================
    @Test
    @DisplayName("Desa un vehicle vàlid i retorna la matrícula")
    void saveVehicle_valid_retornaMatricula() {
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(i -> i.getArgument(0));

        String matricula = vehicleLogic.saveVehicle(vehicleValid());

        assertEquals("1234BCD", matricula);
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("Normalitza matrícula (majúscules, sense espais ni guions) i retalla marca/model/color")
    void saveVehicle_normalitzaDades() {
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(i -> i.getArgument(0));
        Vehicle v = vehicleValid();
        v.setMatricula(" 1234-bcd ");
        v.setMarca("  Seat  ");
        v.setModel(" Ibiza ");
        v.setColor(" Blau ");

        vehicleLogic.saveVehicle(v);

        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(captor.capture());
        assertEquals("1234BCD", captor.getValue().getMatricula());
        assertEquals("Seat", captor.getValue().getMarca());
        assertEquals("Ibiza", captor.getValue().getModel());
        assertEquals("Blau", captor.getValue().getColor());
    }

    @Test
    @DisplayName("Els comentaris privats buits es desen com a null")
    void saveVehicle_comentarisBuitsEsConverteixenANull() {
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(i -> i.getArgument(0));
        Vehicle v = vehicleValid();
        v.setComentaris_privats("    ");

        vehicleLogic.saveVehicle(v);

        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(captor.capture());
        assertNull(captor.getValue().getComentaris_privats());
    }

    @Test
    @DisplayName("Accepta els valors límit permesos")
    void saveVehicle_valorsLimitAcceptats() {
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(i -> i.getArgument(0));
        Vehicle v = vehicleValid();
        v.setPotencia(2000);
        v.setPlaces(20);
        v.setPreu_hores(1000.0);
        v.setFianca(new BigDecimal("100000"));
        v.setMin_dies(1);
        v.setMax_dies(365);
        v.setLimit_km(10000000);
        v.setAny_matriculacio(2001);

        assertEquals("1234BCD", vehicleLogic.saveVehicle(v));
    }

    @Test
    @DisplayName("El límit de quilometratge pot ser null (sense límit)")
    void saveVehicle_limitKmNullEsValid() {
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(i -> i.getArgument(0));
        Vehicle v = vehicleValid();
        v.setLimit_km(null);

        assertEquals("1234BCD", vehicleLogic.saveVehicle(v));
    }

    @Test
    @DisplayName("Un vehicle null llança IllegalArgumentException")
    void saveVehicle_null_llancaExcepcio() {
        assertThrows(IllegalArgumentException.class, () -> vehicleLogic.saveVehicle(null));
        verifyNoInteractions(vehicleRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234ABC", "1234BCQ", "123BCD", "B1234CD", "1234BCDF", "1234BCÑ"})
    @DisplayName("Rebutja matrícules que no són del format espanyol modern")
    void saveVehicle_matriculaInvalida(String matricula) {
        Vehicle v = vehicleValid();
        v.setMatricula(matricula);

        assertSaveError(v, "La matrícula ha de tenir");
    }

    @Test
    @DisplayName("Rebutja una marca massa curta o amb símbols")
    void saveVehicle_marcaInvalida() {
        Vehicle curta = vehicleValid();
        curta.setMarca("S");
        assertSaveError(curta, "La marca ha de tenir");

        Vehicle simbols = vehicleValid();
        simbols.setMarca("Se@t");
        assertSaveError(simbols, "La marca ha de tenir");
    }

    @Test
    @DisplayName("Rebutja un model de més de 50 caràcters")
    void saveVehicle_modelMassaLlarg() {
        Vehicle v = vehicleValid();
        v.setModel("A".repeat(51));

        assertSaveError(v, "El model no pot superar");
    }

    @Test
    @DisplayName("Rebutja un any de matriculació fora de rang")
    void saveVehicle_anyMatriculacioInvalid() {
        Vehicle antic = vehicleValid();
        antic.setAny_matriculacio(VehicleLogic.ANY_MINIM_MATRICULACIO - 1);
        assertSaveError(antic, "L'any de matriculació ha d'estar entre");

        Vehicle futur = vehicleValid();
        futur.setAny_matriculacio(LocalDate.now().getYear() + 1);
        assertSaveError(futur, "L'any de matriculació ha d'estar entre");
    }

    @Test
    @DisplayName("Rebutja una data d'alta futura o anterior a l'any de matriculació")
    void saveVehicle_dataAltaInvalida() {
        Vehicle futura = vehicleValid();
        futura.setData_alta(LocalDate.now().plusDays(1));
        assertSaveError(futura, "La data d'alta no pot ser futura.");

        Vehicle anterior = vehicleValid();
        anterior.setAny_matriculacio(LocalDate.now().getYear());
        anterior.setData_alta(LocalDate.now().minusYears(1));
        assertSaveError(anterior, "anterior a l'any de matriculació");
    }

    @Test
    @DisplayName("Rebutja un preu per hores 0, negatiu, NaN o superior a 1000")
    void saveVehicle_preuHoresInvalid() {
        for (Double preu : new Double[]{0.0, -5.0, Double.NaN, Double.POSITIVE_INFINITY, 1000.01}) {
            Vehicle v = vehicleValid();
            v.setPreu_hores(preu);
            assertSaveError(v, "El preu per hores");
        }
    }

    @Test
    @DisplayName("Rebutja una potència fora de 1-2000 CV")
    void saveVehicle_potenciaInvalida() {
        Vehicle baixa = vehicleValid();
        baixa.setPotencia(0);
        assertSaveError(baixa, "La potència ha d'estar");

        Vehicle alta = vehicleValid();
        alta.setPotencia(2001);
        assertSaveError(alta, "La potència ha d'estar");
    }

    @Test
    @DisplayName("Rebutja un color amb números")
    void saveVehicle_colorInvalid() {
        Vehicle v = vehicleValid();
        v.setColor("Blau3");

        assertSaveError(v, "El color només pot contenir");
    }

    @Test
    @DisplayName("Rebutja un nombre de places fora de 1-20")
    void saveVehicle_placesInvalides() {
        Vehicle zero = vehicleValid();
        zero.setPlaces(VehicleLogic.MIN_PLACES - 1);
        assertSaveError(zero, "El nombre de places");

        Vehicle massa = vehicleValid();
        massa.setPlaces(VehicleLogic.MAX_PLACES + 1);
        assertSaveError(massa, "El nombre de places");
    }

    @Test
    @DisplayName("Rebutja un límit de quilometratge 0 o superior a 10.000.000")
    void saveVehicle_limitKmInvalid() {
        Vehicle zero = vehicleValid();
        zero.setLimit_km(0);
        assertSaveError(zero, "El límit de quilometratge");

        Vehicle massa = vehicleValid();
        massa.setLimit_km(VehicleLogic.LIMIT_QUILOMETRATGE + 1);
        assertSaveError(massa, "El límit de quilometratge");
    }

    @Test
    @DisplayName("Valida el mínim i màxim de dies de lloguer")
    void saveVehicle_diesLloguerInvalids() {
        Vehicle minZero = vehicleValid();
        minZero.setMin_dies(0);
        assertSaveError(minZero, "El mínim de dies");

        Vehicle maxMenorQueMin = vehicleValid();
        maxMenorQueMin.setMin_dies(10);
        maxMenorQueMin.setMax_dies(5);
        assertSaveError(maxMenorQueMin, "no pot ser inferior al mínim");

        Vehicle maxMassaGran = vehicleValid();
        maxMassaGran.setMax_dies(366);
        assertSaveError(maxMassaGran, "no pot superar els 365");
    }

    @Test
    @DisplayName("Valida la fiança: no negativa, màxim 100.000 € i 2 decimals")
    void saveVehicle_fiancaInvalida() {
        Vehicle negativa = vehicleValid();
        negativa.setFianca(new BigDecimal("-1"));
        assertSaveError(negativa, "La fiança no pot ser negativa.");

        Vehicle massaAlta = vehicleValid();
        massaAlta.setFianca(new BigDecimal("100000.01"));
        assertSaveError(massaAlta, "La fiança no pot superar");

        Vehicle tresDecimals = vehicleValid();
        tresDecimals.setFianca(new BigDecimal("10.123"));
        assertSaveError(tresDecimals, "2 decimals");
    }

    @Test
    @DisplayName("Rebutja uns comentaris privats de més de 3000 caràcters")
    void saveVehicle_comentarisMassaLlargs() {
        Vehicle v = vehicleValid();
        v.setComentaris_privats("x".repeat(3001));

        assertSaveError(v, "Els comentaris no poden superar");
    }

    // =====================================================================
    // Consultes bàsiques i matrícula nova
    // =====================================================================
    @Test
    @DisplayName("getVehicleById retorna el vehicle o null si no existeix")
    void getVehicleById() {
        Vehicle v = vehicleValid();
        when(vehicleRepository.findById("1234BCD")).thenReturn(Optional.of(v));

        assertSame(v, vehicleLogic.getVehicleById("1234BCD"));
        assertNull(vehicleLogic.getVehicleById("9999ZZZ"));
    }

    @Test
    @DisplayName("comprovarMatriculaNova llança excepció si la matrícula ja existeix")
    void comprovarMatriculaNova_repetida() {
        when(vehicleRepository.existsById("1234BCD")).thenReturn(true);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> vehicleLogic.comprovarMatriculaNova("1234BCD"));
        assertEquals("Ja existeix un vehicle amb aquesta matrícula.", e.getMessage());
    }

    @Test
    @DisplayName("comprovarMatriculaNova no falla si la matrícula és lliure")
    void comprovarMatriculaNova_lliure() {
        when(vehicleRepository.existsById("1234BCD")).thenReturn(false);

        assertDoesNotThrow(() -> vehicleLogic.comprovarMatriculaNova("1234BCD"));
    }

    @Test
    @DisplayName("existsById amb null retorna false sense consultar la BD")
    void existsById_null() {
        assertFalse(vehicleLogic.existsById(null));
        verifyNoInteractions(vehicleRepository);
    }

    // =====================================================================
    // Estat llogat
    // =====================================================================
    @Test
    @DisplayName("estaLlogat: null no és llogat")
    void estaLlogat_null() {
        assertFalse(vehicleLogic.estaLlogat(null));
        verifyNoInteractions(vehicleRepository, reservaRepository);
    }

    @Test
    @DisplayName("estaLlogat: un vehicle amb estat Llogat ho és")
    void estaLlogat_perEstat() {
        when(vehicleRepository.findById("1234BCD"))
                .thenReturn(Optional.of(vehicleAmb("1234BCD", Enums.EstatVehicle.Llogat)));

        assertTrue(vehicleLogic.estaLlogat("1234BCD"));
    }

    @Test
    @DisplayName("estaLlogat: un vehicle Disponible amb reserva en curs ho és")
    void estaLlogat_perReserva() {
        when(vehicleRepository.findById("1234BCD"))
                .thenReturn(Optional.of(vehicleAmb("1234BCD", Enums.EstatVehicle.Disponible)));
        llogadesAvui("1234BCD");

        assertTrue(vehicleLogic.estaLlogat("1234BCD"));
    }

    @Test
    @DisplayName("estaLlogat: un vehicle Disponible sense reserves en curs no ho és")
    void estaLlogat_cap() {
        when(vehicleRepository.findById("1234BCD"))
                .thenReturn(Optional.of(vehicleAmb("1234BCD", Enums.EstatVehicle.Disponible)));
        llogadesAvui();

        assertFalse(vehicleLogic.estaLlogat("1234BCD"));
    }

    @Test
    @DisplayName("estatVista: Disponible passa a Llogat si té un lloguer en curs")
    void estatVista_disponibleLlogat() {
        llogadesAvui("1234BCD");

        assertEquals("Llogat",
                vehicleLogic.estatVista(vehicleAmb("1234BCD", Enums.EstatVehicle.Disponible)));
    }

    @Test
    @DisplayName("estatVista: manté Disponible sense lloguer i Inactiu sempre")
    void estatVista_mantéEstat() {
        llogadesAvui();
        assertEquals("Disponible",
                vehicleLogic.estatVista(vehicleAmb("1234BCD", Enums.EstatVehicle.Disponible)));
        assertEquals("Inactiu",
                vehicleLogic.estatVista(vehicleAmb("1234BCD", Enums.EstatVehicle.Inactiu)));
    }

    @Test
    @DisplayName("countLlogats compta Llogat o Disponible amb reserva, i ignora els Inactius")
    void countLlogats() {
        when(vehicleRepository.findAll()).thenReturn(List.of(
                vehicleAmb("1111BBB", Enums.EstatVehicle.Llogat),
                vehicleAmb("2222CCC", Enums.EstatVehicle.Disponible),
                vehicleAmb("3333DDD", Enums.EstatVehicle.Inactiu),
                vehicleAmb("4444FFF", Enums.EstatVehicle.Disponible)));
        llogadesAvui("2222CCC", "3333DDD");

        assertEquals(2, vehicleLogic.countLlogats());
    }

    // =====================================================================
    // Eliminació
    // =====================================================================
    @Test
    @DisplayName("No s'elimina un vehicle llogat i no es toquen els seus fitxers")
    void deleteVehicleById_llogat() {
        when(vehicleRepository.findById("1234BCD"))
                .thenReturn(Optional.of(vehicleAmb("1234BCD", Enums.EstatVehicle.Llogat)));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> vehicleLogic.deleteVehicleById("1234BCD"));
        assertTrue(e.getMessage().contains("està llogat actualment"));
        verify(vehicleRepository, never()).deleteById(anyString());
        verifyNoInteractions(arxiuRepository);
    }

    @Test
    @DisplayName("Elimina primer el vehicle i després els seus fitxers privats")
    void deleteVehicleById_ok() {
        when(vehicleRepository.findById("1234BCD"))
                .thenReturn(Optional.of(vehicleAmb("1234BCD", Enums.EstatVehicle.Disponible)));

        vehicleLogic.deleteVehicleById("1234BCD");

        InOrder ordre = inOrder(vehicleRepository, arxiuRepository);
        ordre.verify(vehicleRepository).deleteById("1234BCD");
        ordre.verify(arxiuRepository).deleteByMatricula("1234BCD");
    }

    // =====================================================================
    // Canvi d'estat
    // =====================================================================
    @Test
    @DisplayName("canviaEstat: vehicle inexistent")
    void canviaEstat_inexistent() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> vehicleLogic.canviaEstat("0000XXX", Enums.EstatVehicle.Inactiu));
        assertTrue(e.getMessage().contains("Vehicle inexistent"));
    }

    @Test
    @DisplayName("canviaEstat: l'estat és obligatori")
    void canviaEstat_estatNull() {
        when(vehicleRepository.findById("1234BCD"))
                .thenReturn(Optional.of(vehicleAmb("1234BCD", Enums.EstatVehicle.Disponible)));

        assertThrows(IllegalArgumentException.class, () -> vehicleLogic.canviaEstat("1234BCD", null));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("canviaEstat: no es pot posar Llogat manualment")
    void canviaEstat_llogatManualNoPermes() {
        when(vehicleRepository.findById("1234BCD"))
                .thenReturn(Optional.of(vehicleAmb("1234BCD", Enums.EstatVehicle.Disponible)));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> vehicleLogic.canviaEstat("1234BCD", Enums.EstatVehicle.Llogat));
        assertTrue(e.getMessage().contains("No es pot assignar manualment"));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("canviaEstat: no es pot desactivar un vehicle llogat")
    void canviaEstat_noDesactivarLlogat() {
        when(vehicleRepository.findById("1234BCD"))
                .thenReturn(Optional.of(vehicleAmb("1234BCD", Enums.EstatVehicle.Llogat)));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> vehicleLogic.canviaEstat("1234BCD", Enums.EstatVehicle.Inactiu));
        assertTrue(e.getMessage().contains("No es pot donar de baixa"));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("canviaEstat: desactiva un vehicle disponible sense lloguer")
    void canviaEstat_desactivar() {
        Vehicle v = vehicleAmb("1234BCD", Enums.EstatVehicle.Disponible);
        when(vehicleRepository.findById("1234BCD")).thenReturn(Optional.of(v));

        vehicleLogic.canviaEstat("1234BCD", Enums.EstatVehicle.Inactiu);

        assertEquals(Enums.EstatVehicle.Inactiu, v.getEstat());
        verify(vehicleRepository).save(v);
    }

    @Test
    @DisplayName("canviaEstat: reactiva un vehicle inactiu")
    void canviaEstat_activar() {
        Vehicle v = vehicleAmb("1234BCD", Enums.EstatVehicle.Inactiu);
        when(vehicleRepository.findById("1234BCD")).thenReturn(Optional.of(v));

        vehicleLogic.canviaEstat("1234BCD", Enums.EstatVehicle.Disponible);

        assertEquals(Enums.EstatVehicle.Disponible, v.getEstat());
        verify(vehicleRepository).save(v);
    }

    // =====================================================================
    // Cerca filtrada
    // =====================================================================
    @Test
    @DisplayName("cercaFiltrada: valida la longitud dels filtres de text")
    void cercaFiltrada_textMassaLlarg() {
        assertThrows(IllegalArgumentException.class, () -> vehicleLogic.cercaFiltrada(
                "q".repeat(101), null, null, null, null, null, null, null, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> vehicleLogic.cercaFiltrada(
                null, "m".repeat(21), null, null, null, null, null, null, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> vehicleLogic.cercaFiltrada(
                null, null, "m".repeat(51), null, null, null, null, null, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> vehicleLogic.cercaFiltrada(
                null, null, null, "m".repeat(51), null, null, null, null, 0, 10));
        verifyNoInteractions(vehicleRepository);
    }

    @Test
    @DisplayName("cercaFiltrada: rebutja un any de matriculació invàlid")
    void cercaFiltrada_anyInvalid() {
        assertThrows(IllegalArgumentException.class, () -> vehicleLogic.cercaFiltrada(
                null, null, null, null, 2000, null, null, null, 0, 10));
        verifyNoInteractions(vehicleRepository);
    }

    @Test
    @DisplayName("cercaFiltrada: la data màxima ha de ser posterior a la mínima")
    void cercaFiltrada_datesInvalides() {
        LocalDate data = LocalDate.of(2030, 1, 1);

        assertThrows(IllegalArgumentException.class, () -> vehicleLogic.cercaFiltrada(
                null, null, null, null, null, null, data, data, 0, 10));
        verifyNoInteractions(vehicleRepository);
    }

    @Test
    @DisplayName("cercaFiltrada: limita la pàgina (mínim 0) i la mida (màxim 50)")
    void cercaFiltrada_limitaPaginacio() {
        when(vehicleRepository.findAll(ArgumentMatchers.<Specification<Vehicle>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        vehicleLogic.cercaFiltrada(null, null, null, null, null, null, null, null, -5, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(vehicleRepository).findAll(ArgumentMatchers.<Specification<Vehicle>>any(), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(50, captor.getValue().getPageSize());
    }

    // =====================================================================
    // Validació de fitxers privats (fotos i documents)
    // =====================================================================
    @Test
    @DisplayName("validarArxius: sense fitxers no hi ha error")
    void validarArxius_buit() {
        assertNull(vehicleLogic.validarArxius(null, null, null));
        assertNull(vehicleLogic.validarArxius(null, new MultipartFile[]{fitxer("buit.jpg", "image/jpeg", new byte[0])}, null));
    }

    @Test
    @DisplayName("validarArxius: accepta JPG, PNG, PDF i text vàlids")
    void validarArxius_valids() {
        MultipartFile[] fotos = {fitxer("a.jpg", "image/jpeg", JPG), fitxer("b.png", "image/png", PNG)};
        MultipartFile[] docs = {fitxer("c.pdf", "application/pdf", PDF),
            fitxer("d.txt", "text/plain", "hola".getBytes())};

        assertNull(vehicleLogic.validarArxius(null, fotos, docs));
    }

    @Test
    @DisplayName("validarArxius: rebutja formats no permesos")
    void validarArxius_formatNoPermes() {
        String errorFoto = vehicleLogic.validarArxius(null,
                new MultipartFile[]{fitxer("a.gif", "image/gif", JPG)}, null);
        assertNotNull(errorFoto);
        assertTrue(errorFoto.contains("JPG o PNG"));

        String errorDoc = vehicleLogic.validarArxius(null, null,
                new MultipartFile[]{fitxer("a.docx", "application/msword", PDF)});
        assertNotNull(errorDoc);
        assertTrue(errorDoc.contains("PDF o text"));
    }

    @Test
    @DisplayName("validarArxius: comprova el contingut real i no només el Content-Type")
    void validarArxius_contingutNoCoincideix() {
        String errorPng = vehicleLogic.validarArxius(null,
                new MultipartFile[]{fitxer("fals.png", "image/png", JPG)}, null);
        assertNotNull(errorPng);
        assertTrue(errorPng.contains("no coincideix"));

        String errorText = vehicleLogic.validarArxius(null, null,
                new MultipartFile[]{fitxer("binari.txt", "text/plain", new byte[]{'a', 0, 'b'})});
        assertNotNull(errorText);
        assertTrue(errorText.contains("no coincideix"));
    }

    @Test
    @DisplayName("validarArxius: un fitxer de més de 5 MB és rebutjat")
    void validarArxius_fitxerMassaGran() {
        byte[] gran = ambCapcalera(JPG, CINC_MB + 1);

        String error = vehicleLogic.validarArxius(null,
                new MultipartFile[]{fitxer("gran.jpg", "image/jpeg", gran)}, null);

        assertNotNull(error);
        assertTrue(error.contains("5 MB"));
    }

    @Test
    @DisplayName("validarArxius: màxim 10 fotos i 5 documents")
    void validarArxius_maximFitxers() {
        String errorFotos = vehicleLogic.validarArxius(null, fotosJpg(11), null);
        assertNotNull(errorFotos);
        assertTrue(errorFotos.contains("10 fotografies"));

        assertNull(vehicleLogic.validarArxius(null, fotosJpg(10), null));

        MultipartFile[] sisDocs = new MultipartFile[6];
        for (int i = 0; i < sisDocs.length; i++) {
            sisDocs[i] = fitxer("doc" + i + ".pdf", "application/pdf", PDF);
        }
        String errorDocs = vehicleLogic.validarArxius(null, null, sisDocs);
        assertNotNull(errorDocs);
        assertTrue(errorDocs.contains("5 documents"));
    }

    @Test
    @DisplayName("validarArxius: compta també els fitxers que el vehicle ja té a MongoDB")
    void validarArxius_comptaElsExistents() {
        when(arxiuRepository.countByMatriculaAndTipus("1234BCD", VehicleArxiu.Tipus.FOTO)).thenReturn(10L);
        when(arxiuRepository.countByMatriculaAndTipus("1234BCD", VehicleArxiu.Tipus.DOCUMENT)).thenReturn(0L);

        String error = vehicleLogic.validarArxius("1234BCD", fotosJpg(1), null);

        assertNotNull(error);
        assertTrue(error.contains("10 fotografies"));
    }

    @Test
    @DisplayName("validarArxius: la mida total no pot superar els 25 MB")
    void validarArxius_midaTotalMassaGran() {
        byte[] cincMb = ambCapcalera(JPG, CINC_MB);
        MultipartFile[] fotos = new MultipartFile[5];
        for (int i = 0; i < fotos.length; i++) {
            fotos[i] = fitxer("f" + i + ".jpg", "image/jpeg", cincMb);
        }
        MultipartFile[] docs = {fitxer("extra.pdf", "application/pdf", PDF)};

        String error = vehicleLogic.validarArxius(null, fotos, docs);

        assertNotNull(error);
        assertTrue(error.contains("25 MB"));
    }

    @Test
    @DisplayName("validarFotoPrincipal: accepta PNG, ignora null i rebutja altres formats")
    void validarFotoPrincipal() {
        assertNull(vehicleLogic.validarFotoPrincipal(fitxer("p.png", "image/png", PNG)));
        assertNull(vehicleLogic.validarFotoPrincipal(null));

        String error = vehicleLogic.validarFotoPrincipal(fitxer("p.gif", "image/gif", PNG));
        assertNotNull(error);
        assertTrue(error.startsWith("La foto principal ha de ser en format JPG o PNG"));
    }

    // =====================================================================
    // Desar / recuperar / eliminar fitxers
    // =====================================================================
    @Test
    @DisplayName("desarFotoPrincipal: sense foto no toca la BD")
    void desarFotoPrincipal_buida() throws Exception {
        vehicleLogic.desarFotoPrincipal("1234BCD", null);
        vehicleLogic.desarFotoPrincipal("1234BCD", fitxer("buit.png", "image/png", new byte[0]));

        verifyNoInteractions(arxiuRepository);
    }

    @Test
    @DisplayName("desarFotoPrincipal: substitueix l'anterior i desa la nova com a principal")
    void desarFotoPrincipal_substitueix() throws Exception {
        VehicleArxiu antiga = new VehicleArxiu("1234BCD", VehicleArxiu.Tipus.FOTO,
                "vella.png", "image/png", 8L, PNG);
        antiga.setId("id-antic");
        antiga.setPrincipal(true);
        when(arxiuRepository.findFirstByMatriculaAndTipusAndPrincipalTrue("1234BCD", VehicleArxiu.Tipus.FOTO))
                .thenReturn(Optional.of(antiga));

        vehicleLogic.desarFotoPrincipal("1234BCD", fitxer("nova.png", "image/png", PNG));

        verify(arxiuRepository).deleteById("id-antic");
        ArgumentCaptor<VehicleArxiu> captor = ArgumentCaptor.forClass(VehicleArxiu.class);
        verify(arxiuRepository).save(captor.capture());
        assertTrue(captor.getValue().isPrincipal());
        assertEquals("1234BCD", captor.getValue().getMatricula());
        assertEquals(VehicleArxiu.Tipus.FOTO, captor.getValue().getTipus());
    }

    @Test
    @DisplayName("desarArxius: desa fotos i documents i salta les caselles buides")
    void desarArxius_saltaBuits() throws Exception {
        MultipartFile[] fotos = {fitxer("a.jpg", "image/jpeg", JPG),
            fitxer("buit.jpg", "image/jpeg", new byte[0]),
            fitxer("b.jpg", "image/jpeg", JPG)};
        MultipartFile[] docs = {fitxer("c.pdf", "application/pdf", PDF)};

        vehicleLogic.desarArxius("1234BCD", fotos, docs);

        verify(arxiuRepository, times(3)).save(any(VehicleArxiu.class));
    }

    @Test
    @DisplayName("fotosDe: la foto principal surt la primera")
    void fotosDe_principalPrimera() {
        VehicleArxiu normal = new VehicleArxiu("1234BCD", VehicleArxiu.Tipus.FOTO, "a.jpg", "image/jpeg", 6L, JPG);
        normal.setId("normal");
        VehicleArxiu principal = new VehicleArxiu("1234BCD", VehicleArxiu.Tipus.FOTO, "b.jpg", "image/jpeg", 6L, JPG);
        principal.setId("principal");
        principal.setPrincipal(true);
        when(arxiuRepository.findByMatriculaAndTipusOrderByDataPujadaAsc("1234BCD", VehicleArxiu.Tipus.FOTO))
                .thenReturn(List.of(normal, principal));

        List<VehicleArxiu> fotos = vehicleLogic.fotosDe("1234BCD");

        assertEquals(2, fotos.size());
        assertEquals("principal", fotos.get(0).getId());
        assertEquals("normal", fotos.get(1).getId());
    }

    @Test
    @DisplayName("eliminarArxiu: error si no existeix o si pertany a un altre vehicle")
    void eliminarArxiu_errors() {
        IllegalArgumentException noExisteix = assertThrows(IllegalArgumentException.class,
                () -> vehicleLogic.eliminarArxiu("1234BCD", "id-inexistent"));
        assertEquals("El fitxer no existeix.", noExisteix.getMessage());

        VehicleArxiu arxiuAltre = new VehicleArxiu("9999ZZZ", VehicleArxiu.Tipus.FOTO, "a.jpg", "image/jpeg", 6L, JPG);
        when(arxiuRepository.findById("id-altre")).thenReturn(Optional.of(arxiuAltre));

        IllegalArgumentException noPertany = assertThrows(IllegalArgumentException.class,
                () -> vehicleLogic.eliminarArxiu("1234BCD", "id-altre"));
        assertEquals("El fitxer no pertany a aquest vehicle.", noPertany.getMessage());
        verify(arxiuRepository, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("eliminarArxiu: elimina el fitxer quan pertany al vehicle")
    void eliminarArxiu_ok() {
        VehicleArxiu arxiu = new VehicleArxiu("1234BCD", VehicleArxiu.Tipus.DOCUMENT, "a.pdf", "application/pdf", 8L, PDF);
        when(arxiuRepository.findById("id-ok")).thenReturn(Optional.of(arxiu));

        vehicleLogic.eliminarArxiu("1234BCD", "id-ok");

        verify(arxiuRepository).deleteById("id-ok");
    }
}
# Evently

Android aplikacija za pregled, pretragu, kreiranje i prijavu na događaje, sa korisničkim profilom i podrškom za organizatore i učesnike.


## Pokretanje aplikacije


Aplikaciju je moguće pokrenuti direktno iz Android Studio okruženja pokretanjem `MainActivity` ili pokretanjem cijelog projekta na emulatoru ili fizičkom uređaju.

## Početak rada

Pri pokretanju aplikacije otvara se početni prozor sa login formom.

Korisnik se može prijaviti na dva načina:
- unosom email adrese i lozinke
- prijavom putem Google naloga

Ako aplikaciju koristi prvi put, moguće je registrovati novog korisnika kroz registracijsku formu.

Nakon uspješne prijave otvara se glavni ekran aplikacije.

## Glavne funkcionalnosti

### Home ekran

Na početnom ekranu prikazuje se lista svih dostupnih događaja.

Korisnik može:
- pregledati događaje
- otvoriti detalje događaja
- koristiti search polje za pretragu događaja po nazivu ili gradu
- 
### Event details

Klikom na događaj otvara se ekran sa detaljima događaja.

Na ovom ekranu korisnik može vidjeti:
- naziv događaja
- datum
- grad i lokaciju
- opis događaja
- broj prijavljenih učesnika
- broj prijavljenih volontera
- sliku događaja ako postoji

Ako korisnik nije organizator, može se prijaviti kao učesnik ili kao volonter, ako događaj podržava volontere.

### Kreiranje događaja

Ako je prijavljeni korisnik organizator, na početnom ekranu mu je dostupno dugme za dodavanje događaja.

Prilikom kreiranja događaja moguće je unijeti:
- naziv događaja
- datum preko kalendara
- grad
- lokaciju
- maksimalan broj učesnika
- opcionalno broj volontera
- opis događaja
- sliku iz galerije


### Uređivanje i brisanje događaja

Organizator može otvoriti vlastiti događaj i:
- urediti postojeće podatke
- obrisati događaj

### Profil korisnika

Aplikacija sadrži i ekran profila korisnika.

Na profilu je moguće:
- pregledati osnovne informacije o korisniku
- izvršiti logout iz aplikacije

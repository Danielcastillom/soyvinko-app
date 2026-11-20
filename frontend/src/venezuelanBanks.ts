export interface VenezuelanBank {
  code: string;
  name: string;
  shortName: string;
  pagoMovil: boolean;
}

export const VENEZUELAN_BANKS: VenezuelanBank[] = [
  { code: "0102", name: "Banco de Venezuela, S.A. Banco Universal", shortName: "Banco de Venezuela (BDV)", pagoMovil: true },
  { code: "0104", name: "Venezolano de Crédito, S.A. Banco Universal", shortName: "Venezolano de Crédito", pagoMovil: true },
  { code: "0105", name: "Banco Mercantil, C.A. Banco Universal", shortName: "Mercantil Banco", pagoMovil: true },
  { code: "0108", name: "Banco Provincial, S.A. Banco Universal", shortName: "BBVA Provincial", pagoMovil: true },
  { code: "0114", name: "Bancaribe C.A. Banco Universal", shortName: "Bancaribe", pagoMovil: true },
  { code: "0115", name: "Banco Exterior, C.A. Banco Universal", shortName: "Banco Exterior", pagoMovil: true },
  { code: "0128", name: "Banco Caroní, C.A. Banco Universal", shortName: "Banco Caroní", pagoMovil: true },
  { code: "0134", name: "Banesco Banco Universal, C.A.", shortName: "Banesco", pagoMovil: true },
  { code: "0137", name: "Banco Sofitasa Banco Universal, C.A.", shortName: "Banco Sofitasa", pagoMovil: true },
  { code: "0138", name: "Banco Plaza, Banco Universal", shortName: "Banco Plaza", pagoMovil: true },
  { code: "0151", name: "BFC Banco Fondo Común, C.A. Banco Universal", shortName: "Banco Fondo Común (BFC)", pagoMovil: true },
  { code: "0156", name: "100% Banco, Banco Universal, C.A.", shortName: "100% Banco", pagoMovil: true },
  { code: "0157", name: "DelSur Banco Universal, C.A.", shortName: "DelSur", pagoMovil: true },
  { code: "0163", name: "Banco del Tesoro, C.A. Banco Universal", shortName: "Banco del Tesoro", pagoMovil: true },
  { code: "0166", name: "Banco Agrícola de Venezuela, C.A. Banco Universal", shortName: "Banco Agrícola", pagoMovil: true },
  { code: "0168", name: "Bancrecer, S.A. Banco Microfinanciero", shortName: "Bancrecer", pagoMovil: true },
  { code: "0169", name: "Mi Banco, Banco Microfinanciero, C.A.", shortName: "Mi Banco", pagoMovil: true },
  { code: "0171", name: "Banco Activo, C.A. Banco Universal", shortName: "Banco Activo", pagoMovil: true },
  { code: "0172", name: "Bancamiga Banco Universal, C.A.", shortName: "Bancamiga", pagoMovil: true },
  { code: "0174", name: "Banplus Banco Universal, C.A.", shortName: "Banplus", pagoMovil: true },
  { code: "0175", name: "Banco Bicentenario del Pueblo, Banco Universal, C.A.", shortName: "Banco Bicentenario", pagoMovil: true },
  { code: "0177", name: "Banco de la Fuerza Armada Nacional Bolivariana (BANFANB)", shortName: "BANFANB", pagoMovil: true },
  { code: "0191", name: "Banco Nacional de Crédito, C.A. Banco Universal", shortName: "BNC", pagoMovil: true },
];

export function findBankByCode(code: string): VenezuelanBank | undefined {
  return VENEZUELAN_BANKS.find((b) => b.code === code);
}

export function detectBankFromAccountNumber(account: string): VenezuelanBank | undefined {
  const clean = account.replace(/\D/g, "");
  if (clean.length >= 4) {
    const prefix = clean.substring(0, 4);
    return findBankByCode(prefix);
  }
  return undefined;
}

export function formatBankAccount(account: string): string {
  const clean = account.replace(/\D/g, "").slice(0, 20);
  if (clean.length <= 4) return clean;
  if (clean.length <= 8) return `${clean.slice(0, 4)}-${clean.slice(4)}`;
  if (clean.length <= 10) return `${clean.slice(0, 4)}-${clean.slice(4, 8)}-${clean.slice(8)}`;
  return `${clean.slice(0, 4)}-${clean.slice(4, 8)}-${clean.slice(8, 10)}-${clean.slice(10)}`;
}

export const VENEZUELAN_PHONE_PREFIXES = ["0414", "0424", "0412", "0416", "0426"];

export const DOCUMENT_TYPES = [
  { value: "V", label: "V · Venezolano" },
  { value: "E", label: "E · Extranjero" },
  { value: "J", label: "J · Jurídico / Empresa" },
  { value: "G", label: "G · Gubernamental" },
];

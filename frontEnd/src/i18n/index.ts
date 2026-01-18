import { en } from './translations/en';
import { vi } from './translations/vi';

const translations = {
  en,
  vi,
};

export const useTranslation = (lang: 'en' | 'vi') => {
  return (key: keyof typeof translations['en']) => translations[lang][key] || key;
};

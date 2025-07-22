import dynamic from 'next/dynamic';

// Dynamically import AppWrapper with SSR disabled
const AppWrapper = dynamic(() => import('../components/AppWrapper'), { ssr: false });

export default function Home() {
  return <AppWrapper />;
}

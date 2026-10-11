# NairaRate - Pure Web Version for GitHub Pages & Custom Domains

This directory (`/docs`) contains the pure web browser version of **NairaRate**. It has zero external build steps, works on all browsers (mobile and desktop), and is pre-configured for instant hosting on **GitHub Pages**, **Vercel**, **Netlify**, or any custom domain.

## How to Host on GitHub with Your Own Domain (Free)

### 1. Enable GitHub Pages
1. Push this repository to GitHub (or use the **GitHub** button in the AI Studio top bar).
2. On your GitHub repository page, go to **Settings** → **Pages** (under "Code and automation").
3. Under **Build and deployment**:
   - **Source**: Select `Deploy from a branch`
   - **Branch**: Select `main` (or `master`) and change the folder from `/(root)` to **`/docs`**.
   - Click **Save**.
4. GitHub will give you an instant live URL: `https://<your-username>.github.io/<repo-name>/`.

### 2. Connect Your Custom Domain
1. In the same **Settings** → **Pages** tab, scroll to **Custom domain**.
2. Enter your domain name (e.g., `nairarate.com` or `www.nairarate.com`).
3. Click **Save**. GitHub will automatically generate a free HTTPS SSL certificate.
4. In your domain registrar (GoDaddy, Namecheap, Cloudflare, etc.), add DNS records:
   - For an apex domain (`example.com`): Create `A` records pointing to GitHub's IPs:
     ```
     185.199.108.153
     185.199.109.153
     185.199.110.153
     185.199.111.153
     ```
   - For `www` subdomain: Create a `CNAME` record pointing to `<your-username>.github.io`.

## Features Included in the Pure Web Version
- **Live Real-Time Market Rates**: Fetches live USD/NGN exchange rates directly from open exchange rate APIs.
- **Bidirectional Converter**: Live calculator with instant swap ($ ⇄ ₦) and one-tap copy.
- **Cash & Banknote Breakdown**: Computes exact payout in ₦1,000, ₦500 notes and ₦100k bundles.
- **Working Account**: Dual NGN & USD balances with local storage persistence.
- **Send Money**: Send NGN or USD to Nigerian banks or P2P tags with real-time conversion previews.
- **Receive Money**: Dedicated account number (`9041285372`), simulated QR code, and one-tap copy.
- **In-Wallet Conversion**: Convert between USD and NGN balances at market rates.
- **Transaction Stream**: Real-time log of sent, received, and converted funds.

// Regenerates local-maven/net/neoforged/minecraft-dependencies/<version>/...pom
//
// Why this exists
// ---------------
// NeoForge's build pipeline resolves `net.neoforged:minecraft-dependencies:<mc version>`: a
// *generated* POM that simply lists the libraries Mojang ships with Minecraft. Some NeoForged
// mirrors (including the CDN mirror this project falls back to) do not carry it, and
// maven.neoforged.net may be unreachable from your network. Because the artifact is derived data,
// we can rebuild an equivalent POM from the official Mojang version manifest.
//
// Usage
// -----
//   node tools/gen-mcdeps.js            # fetches the manifest and writes the POM
//   node tools/gen-mcdeps.js --check    # only reports whether the POM is up to date
//
// The manifest is fetched from Mojang's own metadata host with a redirect-following request; if
// that host is blocked, download the JSON by hand and pass --manifest <file>.

const fs = require('fs');
const path = require('path');
const https = require('https');

const MC_VERSION = process.env.MC_VERSION || '26.2';
const root = path.resolve(__dirname, '..');
const outDir = path.join(root, 'local-maven', 'net', 'neoforged', 'minecraft-dependencies', MC_VERSION);
const outFile = path.join(outDir, `minecraft-dependencies-${MC_VERSION}.pom`);
const args = process.argv.slice(2);
const checkOnly = args.includes('--check');
const manifestArg = args.indexOf('--manifest');
const localManifest = manifestArg >= 0 ? args[manifestArg + 1] : null;

const VERSION_MANIFEST = `https://piston-meta.mojang.com/mc/game/version_manifest_v2.json`;
const CDN_FALLBACK = `https://bmclapi2.bangbang93.com/version/${MC_VERSION}/json`;

function get(url, redirects = 0) {
  return new Promise((resolve, reject) => {
    https.get(url, { headers: { 'user-agent': 'more-fireworks-build/1.0' } }, res => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location && redirects < 5) {
        res.resume();
        resolve(get(new URL(res.headers.location, url).toString(), redirects + 1));
        return;
      }
      if (res.statusCode !== 200) {
        res.resume();
        reject(new Error(`GET ${url} -> HTTP ${res.statusCode}`));
        return;
      }
      let data = '';
      res.setEncoding('utf8');
      res.on('data', chunk => (data += chunk));
      res.on('end', () => resolve(data));
    }).on('error', reject);
  });
}

async function loadManifest() {
  if (localManifest) {
    return JSON.parse(fs.readFileSync(localManifest, 'utf8'));
  }
  try {
    const index = JSON.parse(await get(VERSION_MANIFEST));
    const entry = index.versions.find(v => v.id === MC_VERSION);
    if (!entry) throw new Error(`version ${MC_VERSION} not in the manifest index`);
    return JSON.parse(await get(entry.url));
  } catch (error) {
    console.warn(`primary metadata host failed (${error.message}); trying the BMCLAPI mirror`);
    return JSON.parse(await get(CDN_FALLBACK));
  }
}

function buildPom(manifest) {
  const seen = new Set();
  const deps = [];
  for (const library of manifest.libraries) {
    const parts = library.name.split(':');
    if (parts.length < 3) continue;
    const [group, artifact, version, classifier] = parts;
    const key = `${group}:${artifact}:${version}:${classifier ?? ''}`;
    if (seen.has(key)) continue;
    seen.add(key);
    deps.push({ group, artifact, version, classifier });
  }
  deps.sort((a, b) => (a.group + ':' + a.artifact).localeCompare(b.group + ':' + b.artifact));

  const body = deps.map(d => `    <dependency>
      <groupId>${d.group}</groupId>
      <artifactId>${d.artifact}</artifactId>
      <version>${d.version}</version>${d.classifier ? `\n      <classifier>${d.classifier}</classifier>` : ''}
      <scope>compile</scope>
    </dependency>`).join('\n');

  return `<?xml version="1.0" encoding="UTF-8"?>
<!--
  Generated file - do not edit.
  Regenerate with: node tools/gen-mcdeps.js
  Source: Minecraft ${MC_VERSION} version manifest (javaVersion ${manifest.javaVersion && manifest.javaVersion.majorVersion})
  Libraries: ${deps.length}
-->
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>net.neoforged</groupId>
  <artifactId>minecraft-dependencies</artifactId>
  <version>${MC_VERSION}</version>
  <packaging>pom</packaging>
  <name>Minecraft ${MC_VERSION} dependencies (local stand-in)</name>
  <description>
    Stand-in for the NeoForged generated POM of the same coordinates, rebuilt from
    Mojang's published version manifest so the build works on networks where the
    NeoForged Maven is unreachable and its mirrors do not carry this file.
  </description>
  <dependencies>
${body}
  </dependencies>
</project>
`;
}

(async () => {
  const manifest = await loadManifest();
  const pom = buildPom(manifest);
  if (checkOnly) {
    const current = fs.existsSync(outFile) ? fs.readFileSync(outFile, 'utf8') : '';
    console.log(current === pom ? `${outFile} is up to date` : `${outFile} is stale; rerun without --check`);
    process.exitCode = current === pom ? 0 : 1;
    return;
  }
  fs.mkdirSync(outDir, { recursive: true });
  fs.writeFileSync(outFile, pom);
  console.log(`wrote ${outFile} (${pom.split('\n').length} lines)`);
})().catch(error => {
  console.error(error.message);
  process.exitCode = 1;
});

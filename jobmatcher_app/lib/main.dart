import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

void main() {
  runApp(const MyApp());
}

class AppState extends ChangeNotifier {
  String? token;
  List<String> roles = [];
  String baseUrl = 'http://localhost:8080';

  bool get isAuthed => token != null;

  void setAuth(String t, List<String> r) {
    token = t;
    roles = r;
    notifyListeners();
  }

  void logout() {
    token = null;
    roles = [];
    notifyListeners();
  }

  bool hasRole(String role) => roles.contains(role);
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});
  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final AppState app = AppState();

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: app,
      builder: (_, __) {
        return MaterialApp(
          debugShowCheckedModeBanner: false,
          title: 'JobMatcher Demo',
          theme: ThemeData(useMaterial3: true),
          home: app.isAuthed ? HomePage(app: app) : AuthPage(app: app),
        );
      },
    );
  }
}

class ApiClient {
  ApiClient({required this.baseUrl, this.token});
  final String baseUrl;
  final String? token;

  Map<String, String> _headers({bool json = true}) {
    final h = <String, String>{};
    if (json) h['Content-Type'] = 'application/json';
    if (token != null) h['Authorization'] = 'Bearer $token';
    return h;
  }

  Future<Map<String, dynamic>> login(String username, String password) async {
    final res = await http.post(
      Uri.parse('$baseUrl/api/auth/login'),
      headers: _headers(),
      body: jsonEncode({'username': username, 'password': password}),
    );
    if (res.statusCode >= 400) {
      throw Exception(res.body.isNotEmpty ? res.body : 'Login failed');
    }
    return jsonDecode(res.body) as Map<String, dynamic>;
  }

  Future<Map<String, dynamic>> register(String username, String email, String password, String role) async {
    final res = await http.post(
      Uri.parse('$baseUrl/api/auth/register'),
      headers: _headers(),
      body: jsonEncode({
        'username': username,
        'email': email,
        'password': password,
        'role': role,
      }),
    );
    if (res.statusCode >= 400) {
      throw Exception(res.body.isNotEmpty ? res.body : 'Register failed');
    }
    return jsonDecode(res.body) as Map<String, dynamic>;
  }

  Future<List<dynamic>> swipeFeed({double? lat, double? lon, double? radiusKm, int? limit}) async {
    final q = <String, String>{};
    if (lat != null) q['lat'] = '$lat';
    if (lon != null) q['lon'] = '$lon';
    if (radiusKm != null) q['radiusKm'] = '$radiusKm';
    if (limit != null) q['limit'] = '$limit';

    final uri = Uri.parse('$baseUrl/api/candidates/me/swipe-feed').replace(queryParameters: q);
    final res = await http.get(uri, headers: _headers(json: false));
    if (res.statusCode >= 400) throw Exception(res.body.isNotEmpty ? res.body : 'Swipe feed failed');
    return jsonDecode(res.body) as List<dynamic>;
  }

  Future<Map<String, dynamic>> swipe(String jobId, String action) async {
    final res = await http.post(
      Uri.parse('$baseUrl/api/candidates/me/swipes'),
      headers: _headers(),
      body: jsonEncode({'jobId': jobId, 'action': action}),
    );
    if (res.statusCode >= 400) throw Exception(res.body.isNotEmpty ? res.body : 'Swipe failed');
    return jsonDecode(res.body) as Map<String, dynamic>;
  }

  Future<List<dynamic>> matches({double? lat, double? lon, double? radiusKm, int? limit}) async {
    final q = <String, String>{};
    if (lat != null) q['lat'] = '$lat';
    if (lon != null) q['lon'] = '$lon';
    if (radiusKm != null) q['radiusKm'] = '$radiusKm';
    if (limit != null) q['limit'] = '$limit';

    final uri = Uri.parse('$baseUrl/api/candidates/me/matches').replace(queryParameters: q);
    final res = await http.get(uri, headers: _headers(json: false));
    if (res.statusCode >= 400) throw Exception(res.body.isNotEmpty ? res.body : 'Matches failed');
    return jsonDecode(res.body) as List<dynamic>;
  }

  Future<List<dynamic>> companyJobsMine() async {
    final res = await http.get(
      Uri.parse('$baseUrl/api/jobs/mine'),
      headers: _headers(json: false),
    );
    if (res.statusCode >= 400) throw Exception(res.body.isNotEmpty ? res.body : 'Jobs mine failed');
    return jsonDecode(res.body) as List<dynamic>;
  }

  Future<Map<String, dynamic>> createJob(Map<String, dynamic> payload) async {
    final res = await http.post(
      Uri.parse('$baseUrl/api/jobs'),
      headers: _headers(),
      body: jsonEncode(payload),
    );
    if (res.statusCode >= 400) throw Exception(res.body.isNotEmpty ? res.body : 'Create job failed');
    return jsonDecode(res.body) as Map<String, dynamic>;
  }
}

class AuthPage extends StatefulWidget {
  const AuthPage({super.key, required this.app});
  final AppState app;

  @override
  State<AuthPage> createState() => _AuthPageState();
}

class _AuthPageState extends State<AuthPage> {
  int tab = 0;

  final _loginUser = TextEditingController(text: 'dev');
  final _loginPass = TextEditingController(text: 'dev123');

  final _regUser = TextEditingController();
  final _regEmail = TextEditingController();
  final _regPass = TextEditingController(text: 'Password123!');
  String _regRole = 'CANDIDATE';

  bool loading = false;

  @override
  void dispose() {
    _loginUser.dispose();
    _loginPass.dispose();
    _regUser.dispose();
    _regEmail.dispose();
    _regPass.dispose();
    super.dispose();
  }

  Future<void> _doLogin() async {
    setState(() => loading = true);
    try {
      final api = ApiClient(baseUrl: widget.app.baseUrl);
      final res = await api.login(_loginUser.text.trim(), _loginPass.text);
      final token = res['token'] as String;
      final roles = (res['roles'] as List<dynamic>).map((e) => e.toString()).toList();
      if (!mounted) return;
      widget.app.setAuth(token, roles);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Login error: $e')));
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> _doRegister() async {
    setState(() => loading = true);
    try {
      final api = ApiClient(baseUrl: widget.app.baseUrl);
      final res = await api.register(
        _regUser.text.trim(),
        _regEmail.text.trim(),
        _regPass.text,
        _regRole,
      );
      final token = res['token'] as String;
      final roles = (res['roles'] as List<dynamic>).map((e) => e.toString()).toList();
      if (!mounted) return;
      widget.app.setAuth(token, roles);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Register error: $e')));
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final isLogin = tab == 0;

    return Scaffold(
      appBar: AppBar(
        title: const Text('JobMatcher Demo'),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(48),
          child: Row(
            children: [
              Expanded(
                child: TextButton(
                  onPressed: loading ? null : () => setState(() => tab = 0),
                  child: Text('Login', style: TextStyle(fontWeight: isLogin ? FontWeight.bold : FontWeight.normal)),
                ),
              ),
              Expanded(
                child: TextButton(
                  onPressed: loading ? null : () => setState(() => tab = 1),
                  child: Text('Register', style: TextStyle(fontWeight: !isLogin ? FontWeight.bold : FontWeight.normal)),
                ),
              ),
            ],
          ),
        ),
      ),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 520),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: isLogin ? _loginForm() : _registerForm(),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _loginForm() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        TextField(controller: _loginUser, decoration: const InputDecoration(labelText: 'Username')),
        const SizedBox(height: 12),
        TextField(
          controller: _loginPass,
          decoration: const InputDecoration(labelText: 'Password'),
          obscureText: true,
        ),
        const SizedBox(height: 16),
        SizedBox(
          width: double.infinity,
          child: FilledButton(
            onPressed: loading ? null : _doLogin,
            child: loading ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator()) : const Text('Login'),
          ),
        ),
        const SizedBox(height: 8),
        const Text('Tip: dev / dev123'),
      ],
    );
  }

  Widget _registerForm() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        TextField(controller: _regUser, decoration: const InputDecoration(labelText: 'Username')),
        const SizedBox(height: 12),
        TextField(controller: _regEmail, decoration: const InputDecoration(labelText: 'Email')),
        const SizedBox(height: 12),
        TextField(
          controller: _regPass,
          decoration: const InputDecoration(labelText: 'Password'),
          obscureText: true,
        ),
        const SizedBox(height: 12),
        DropdownButtonFormField<String>(
          initialValue: _regRole,
          items: const [
            DropdownMenuItem(value: 'CANDIDATE', child: Text('CANDIDATE')),
            DropdownMenuItem(value: 'COMPANY', child: Text('COMPANY')),
          ],
          onChanged: loading ? null : (v) => setState(() => _regRole = v ?? 'CANDIDATE'),
          decoration: const InputDecoration(labelText: 'Role'),
        ),
        const SizedBox(height: 16),
        SizedBox(
          width: double.infinity,
          child: FilledButton(
            onPressed: loading ? null : _doRegister,
            child: loading ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator()) : const Text('Register'),
          ),
        ),
      ],
    );
  }
}

class HomePage extends StatelessWidget {
  const HomePage({super.key, required this.app});
  final AppState app;

  @override
  Widget build(BuildContext context) {
    final isCandidate = app.hasRole('CANDIDATE');
    final isCompany = app.hasRole('COMPANY');
    final isDev = app.hasRole('DEV') || app.hasRole('ADMIN');

    // DEV: scegli home in base a quello che vuoi mostrare (qui: Candidate)
    if (isCandidate || isDev) return CandidateHome(app: app);
    if (isCompany) return CompanyHome(app: app);
    return Scaffold(
      appBar: AppBar(title: const Text('Home')),
      body: const Center(child: Text('No role')),
    );
  }
}

class CandidateHome extends StatefulWidget {
  const CandidateHome({super.key, required this.app});
  final AppState app;

  @override
  State<CandidateHome> createState() => _CandidateHomeState();
}

class _CandidateHomeState extends State<CandidateHome> {
  int idx = 0;
  double? lat = 45.4642;
  double? lon = 9.19;
  double radiusKm = 50;

  @override
  Widget build(BuildContext context) {
    final pages = [
      SwipePage(app: widget.app, lat: lat, lon: lon, radiusKm: radiusKm),
      MatchesPage(app: widget.app, lat: lat, lon: lon, radiusKm: radiusKm),
      SettingsPage(
        onLogout: widget.app.logout,
        lat: lat,
        lon: lon,
        radiusKm: radiusKm,
        onChanged: (a, b, r) => setState(() {
          lat = a;
          lon = b;
          radiusKm = r;
        }),
      ),
    ];

    return Scaffold(
      appBar: AppBar(title: const Text('Candidate')),
      body: pages[idx],
      bottomNavigationBar: NavigationBar(
        selectedIndex: idx,
        onDestinationSelected: (v) => setState(() => idx = v),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.swipe), label: 'Swipe'),
          NavigationDestination(icon: Icon(Icons.star), label: 'Matches'),
          NavigationDestination(icon: Icon(Icons.settings), label: 'Settings'),
        ],
      ),
    );
  }
}

class SwipePage extends StatefulWidget {
  const SwipePage({super.key, required this.app, this.lat, this.lon, required this.radiusKm});
  final AppState app;
  final double? lat;
  final double? lon;
  final double radiusKm;

  @override
  State<SwipePage> createState() => _SwipePageState();
}

class _SwipePageState extends State<SwipePage> {
  bool loading = false;
  List<dynamic> items = [];
  int cursor = 0;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => loading = true);
    try {
      final api = ApiClient(baseUrl: widget.app.baseUrl, token: widget.app.token);
      final res = await api.swipeFeed(lat: widget.lat, lon: widget.lon, radiusKm: widget.radiusKm, limit: 20);
      if (!mounted) return;
      setState(() {
        items = res;
        cursor = 0;
      });
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Swipe feed error: $e')));
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> _swipe(String action) async {
    if (cursor >= items.length) return;
    final current = items[cursor] as Map<String, dynamic>;
    final job = current['job'] as Map<String, dynamic>;
    final jobId = job['id'] as String;

    try {
      final api = ApiClient(baseUrl: widget.app.baseUrl, token: widget.app.token);
      await api.swipe(jobId, action);
      if (!mounted) return;
      setState(() {
        cursor += 1;
      });
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Swipe error: $e')));
    }
  }

  @override
  Widget build(BuildContext context) {
    if (loading) return const Center(child: CircularProgressIndicator());
    if (items.isEmpty || cursor >= items.length) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('Nessun job nello swipe feed (o li hai già swipati).'),
            const SizedBox(height: 12),
            FilledButton(onPressed: _load, child: const Text('Ricarica')),
          ],
        ),
      );
    }

    final current = items[cursor] as Map<String, dynamic>;
    final job = current['job'] as Map<String, dynamic>;
    final distance = current['distanceKm'];

    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        children: [
          Expanded(child: _JobCard(job: job, distanceKm: distance)),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () => _swipe('DISLIKE'),
                  icon: const Icon(Icons.close),
                  label: const Text('DISLIKE'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: FilledButton.icon(
                  onPressed: () => _swipe('LIKE'),
                  icon: const Icon(Icons.favorite),
                  label: const Text('LIKE'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _JobCard extends StatelessWidget {
  const _JobCard({required this.job, this.distanceKm});
  final Map<String, dynamic> job;
  final dynamic distanceKm;

  @override
  Widget build(BuildContext context) {
    final title = (job['title'] ?? '').toString();
    final desc = (job['description'] ?? '').toString();
    final location = (job['location'] ?? '').toString();
    final applyUrl = (job['applyUrl'] ?? '').toString();
    final embedded = (job['embedded'] ?? false) == true;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: ListView(
          children: [
            Text(title, style: Theme.of(context).textTheme.headlineSmall),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                if (location.isNotEmpty) Chip(label: Text(location)),
                if (distanceKm != null) Chip(label: Text('${distanceKm.toString()} km')),
                Chip(label: Text(embedded ? 'embedded' : 'no-embed')),
              ],
            ),
            const SizedBox(height: 12),
            Text(desc.isEmpty ? '(no description)' : desc),
            const SizedBox(height: 12),
            if (applyUrl.isNotEmpty)
              SelectableText('Apply: $applyUrl', style: const TextStyle(fontSize: 12)),
          ],
        ),
      ),
    );
  }
}

class MatchesPage extends StatefulWidget {
  const MatchesPage({super.key, required this.app, this.lat, this.lon, required this.radiusKm});
  final AppState app;
  final double? lat;
  final double? lon;
  final double radiusKm;

  @override
  State<MatchesPage> createState() => _MatchesPageState();
}

class _MatchesPageState extends State<MatchesPage> {
  bool loading = false;
  List<dynamic> items = [];

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => loading = true);
    try {
      final api = ApiClient(baseUrl: widget.app.baseUrl, token: widget.app.token);
      final res = await api.matches(lat: widget.lat, lon: widget.lon, radiusKm: widget.radiusKm, limit: 50);
      if (!mounted) return;
      setState(() => items = res);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Matches error: $e')));
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (loading) return const Center(child: CircularProgressIndicator());
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.builder(
        padding: const EdgeInsets.all(12),
        itemCount: items.isEmpty ? 1 : items.length,
        itemBuilder: (_, i) {
          if (items.isEmpty) {
            return const Padding(
              padding: EdgeInsets.only(top: 80),
              child: Center(child: Text('Nessun match. Fai LIKE nello swipe feed.')),
            );
          }
          final it = items[i] as Map<String, dynamic>;
          final job = it['job'] as Map<String, dynamic>;
          final score = it['score'];
          final distance = it['distanceKm'];
          final reasons = (it['reasons'] as List<dynamic>? ?? []).map((e) => e.toString()).toList();

          return Card(
            child: ListTile(
              title: Text((job['title'] ?? '').toString()),
              subtitle: Text('score: $score | dist: $distance | reasons: ${reasons.join(", ")}'),
            ),
          );
        },
      ),
    );
  }
}

class CompanyHome extends StatefulWidget {
  const CompanyHome({super.key, required this.app});
  final AppState app;

  @override
  State<CompanyHome> createState() => _CompanyHomeState();
}

class _CompanyHomeState extends State<CompanyHome> {
  int idx = 0;

  @override
  Widget build(BuildContext context) {
    final pages = [
      CompanyJobsPage(app: widget.app),
      CreateJobPage(app: widget.app),
      SettingsPage(onLogout: widget.app.logout),
    ];

    return Scaffold(
      appBar: AppBar(title: const Text('Company')),
      body: pages[idx],
      bottomNavigationBar: NavigationBar(
        selectedIndex: idx,
        onDestinationSelected: (v) => setState(() => idx = v),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.list), label: 'My Jobs'),
          NavigationDestination(icon: Icon(Icons.add), label: 'Create'),
          NavigationDestination(icon: Icon(Icons.settings), label: 'Settings'),
        ],
      ),
    );
  }
}

class CompanyJobsPage extends StatefulWidget {
  const CompanyJobsPage({super.key, required this.app});
  final AppState app;

  @override
  State<CompanyJobsPage> createState() => _CompanyJobsPageState();
}

class _CompanyJobsPageState extends State<CompanyJobsPage> {
  bool loading = false;
  List<dynamic> jobs = [];

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => loading = true);
    try {
      final api = ApiClient(baseUrl: widget.app.baseUrl, token: widget.app.token);
      final res = await api.companyJobsMine();
      if (!mounted) return;
      setState(() => jobs = res);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Jobs mine error: $e')));
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (loading) return const Center(child: CircularProgressIndicator());
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView.builder(
        padding: const EdgeInsets.all(12),
        itemCount: jobs.isEmpty ? 1 : jobs.length,
        itemBuilder: (_, i) {
          if (jobs.isEmpty) return const Padding(padding: EdgeInsets.only(top: 80), child: Center(child: Text('No jobs yet')));
          final j = jobs[i] as Map<String, dynamic>;
          return Card(
            child: ListTile(
              title: Text((j['title'] ?? '').toString()),
              subtitle: Text('status: ${j['status']} | embedded: ${j['embedded']}'),
            ),
          );
        },
      ),
    );
  }
}

class CreateJobPage extends StatefulWidget {
  const CreateJobPage({super.key, required this.app});
  final AppState app;

  @override
  State<CreateJobPage> createState() => _CreateJobPageState();
}

class _CreateJobPageState extends State<CreateJobPage> {
  final title = TextEditingController();
  final description = TextEditingController();
  final location = TextEditingController(text: 'Milano');
  final lat = TextEditingController(text: '45.4642');
  final lon = TextEditingController(text: '9.19');
  bool loading = false;

  @override
  void dispose() {
    title.dispose();
    description.dispose();
    location.dispose();
    lat.dispose();
    lon.dispose();
    super.dispose();
  }

  Future<void> _create() async {
    setState(() => loading = true);
    try {
      final api = ApiClient(baseUrl: widget.app.baseUrl, token: widget.app.token);
      final payload = {
        'title': title.text.trim(),
        'description': description.text.trim(),
        'location': location.text.trim(),
        'lat': double.tryParse(lat.text.trim()),
        'lon': double.tryParse(lon.text.trim()),
        'status': 'PUBLISHED',
        'applyUrl': 'https://example.com',
      };
      await api.createJob(payload);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Job created')));
      title.clear();
      description.clear();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Create job error: $e')));
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: ListView(
        children: [
          TextField(controller: title, decoration: const InputDecoration(labelText: 'Title')),
          const SizedBox(height: 12),
          TextField(controller: description, decoration: const InputDecoration(labelText: 'Description'), maxLines: 4),
          const SizedBox(height: 12),
          TextField(controller: location, decoration: const InputDecoration(labelText: 'Location')),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(child: TextField(controller: lat, decoration: const InputDecoration(labelText: 'Lat'))),
              const SizedBox(width: 12),
              Expanded(child: TextField(controller: lon, decoration: const InputDecoration(labelText: 'Lon'))),
            ],
          ),
          const SizedBox(height: 16),
          FilledButton(
            onPressed: loading ? null : _create,
            child: loading ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator()) : const Text('Create (PUBLISHED)'),
          ),
        ],
      ),
    );
  }
}

class SettingsPage extends StatefulWidget {
  const SettingsPage({
    super.key,
    required this.onLogout,
    this.lat,
    this.lon,
    this.radiusKm,
    this.onChanged,
  });

  final VoidCallback onLogout;
  final double? lat;
  final double? lon;
  final double? radiusKm;
  final void Function(double? lat, double? lon, double radiusKm)? onChanged;

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  late TextEditingController latCtl;
  late TextEditingController lonCtl;
  late TextEditingController radCtl;

  @override
  void initState() {
    super.initState();
    latCtl = TextEditingController(text: widget.lat?.toString() ?? '');
    lonCtl = TextEditingController(text: widget.lon?.toString() ?? '');
    radCtl = TextEditingController(text: (widget.radiusKm ?? 50).toString());
  }

  @override
  void dispose() {
    latCtl.dispose();
    lonCtl.dispose();
    radCtl.dispose();
    super.dispose();
  }

  void _apply() {
    final a = latCtl.text.trim().isEmpty ? null : double.tryParse(latCtl.text.trim());
    final b = lonCtl.text.trim().isEmpty ? null : double.tryParse(lonCtl.text.trim());
    final r = double.tryParse(radCtl.text.trim()) ?? 50;
    widget.onChanged?.call(a, b, r);
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Settings updated')));
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: ListView(
        children: [
          const Text('Geo params (demo)'),
          const SizedBox(height: 12),
          TextField(controller: latCtl, decoration: const InputDecoration(labelText: 'Lat (optional)')),
          const SizedBox(height: 12),
          TextField(controller: lonCtl, decoration: const InputDecoration(labelText: 'Lon (optional)')),
          const SizedBox(height: 12),
          TextField(controller: radCtl, decoration: const InputDecoration(labelText: 'Radius km')),
          const SizedBox(height: 12),
          OutlinedButton(onPressed: _apply, child: const Text('Apply')),
          const SizedBox(height: 24),
          FilledButton.tonal(onPressed: widget.onLogout, child: const Text('Logout')),
        ],
      ),
    );
  }
}

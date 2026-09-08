package com.example.data.local

data class SampleDoc(
    val title: String,
    val category: String,
    val content: String
)

object SampleDocuments {
    val list = listOf(
        SampleDoc(
            title = "Acme Corp 2026 Employee Policy & Remote Guide",
            category = "HR & Policy",
            content = """
Acme Corporation Global Employee Handbook & Remote Work Standards (2026 Edition).

1. Working Hours & Collaboration:
Employees operate under a hybrid and remote-first framework. Core collaboration hours are 10:00 AM to 3:00 PM Eastern Time, during which team syncs, sprint planning, and client meetings are scheduled. Outside core hours, team members have complete flexibility over their working hours.

2. Equipment & Remote Work Stipends:
All full-time employees receive a one-time $800 home office stipend upon hiring for ergonomic chairs, standing desks, or dual monitor setups. Additionally, a recurring monthly stipend of $100 is automatically credited on pay stubs to offset high-speed fiber internet and mobile phone expenses.

3. Paid Time Off (PTO) & Leave Policies:
Full-time personnel receive 20 days of paid annual vacation, accrued per pay period. In addition, Acme observes 12 paid public holidays and provides 2 floating cultural or personal observance days. Unused PTO can carry over up to a maximum of 5 days into the following calendar year. Paid parental leave provides 16 fully compensated weeks for primary and secondary caregivers following birth, adoption, or foster placement.

4. 401(k) Retirement & Financial Perks:
Acme sponsors a traditional and Roth 401(k) plan. Acme matches 100% of employee contributions up to 6% of base salary, with immediate 100% vesting from day one of employment. 

5. Healthcare & Wellness Benefit:
Employees have access to comprehensive health coverage (PPO and High-Deductible Health Plans with $1,000 employer HSA annual funding). Each employee receives a $1,200 annual Wellness Stipend, reimbursable for gym memberships, fitness trackers, ski passes, ergonomic equipment, and mental health subscriptions like Headspace or Calm.
            """.trimIndent()
        ),
        SampleDoc(
            title = "Android Jetpack & Modern Architecture Guide",
            category = "Engineering",
            content = """
Architectural Principles for Robust and Scalable Android Applications (Android Jetpack 2026).

1. Layered Architecture:
Modern Android apps separate concerns into three core layers:
- The UI Layer: Composables and ViewModels that format application data for the screen.
- The Optional Domain Layer: Encapsulates complex business logic or coordinates multiple repositories via UseCases.
- The Data Layer: Repositories and Data Sources (Local Room Database, Remote APIs) providing a single source of truth (SSOT).

2. Unidirectional Data Flow (UDF):
State flows downwards from ViewModel state holders to Composables as immutable StateFlow objects, consumed via collectAsStateWithLifecycle(). Events (user interactions, taps, text changes) flow upwards to the ViewModel through simple lambda callbacks, ensuring deterministic UI rendering and effortless testability.

3. Local Persistence with Room:
Room provides an abstraction layer over SQLite, verifying SQL queries at compile time with KSP. DAOs return reactive Flow<List<T>> instances, allowing UI screens to automatically refresh whenever the underlying database records mutate.

4. Asynchronous Concurrency with Coroutines:
All blocking I/O (disk reads, database queries, network requests) must run on Dispatchers.IO to maintain a smooth 60/120 FPS interface on the main UI thread. CPU-intensive operations (vector distance, image compression) should leverage Dispatchers.Default.

5. Jetpack Compose Performance:
Composables should use remember and derivedStateOf to prevent unnecessary recompositions during state updates. Key parameters in LazyColumn ensure smooth list recycling without layout jitter.
            """.trimIndent()
        ),
        SampleDoc(
            title = "Quantum Computing & PQC Cryptography Spec",
            category = "Security",
            content = """
Technical Overview of Post-Quantum Cryptography (PQC) and Quantum Computing Threats.

1. Quantum Fundamentals:
Quantum computers leverage qubits that exist in superpositions of 0 and 1, governed by quantum mechanical principles of entanglement and interference. This enables quantum algorithms to process vast combinatorial state spaces simultaneously.

2. Threat to Classical Cryptography:
Peter Shor's 1994 quantum algorithm demonstrates that a fault-tolerant quantum computer can compute discrete logarithms and factor large integers in polynomial time. Consequently, asymmetric cryptographic algorithms safeguarding the modern internet—including RSA-2048, Diffie-Hellman, and Elliptic Curve Cryptography (ECDSA, Ed25519)—are fundamentally broken by cryptographically relevant quantum computers (CRQCs).

3. NIST Post-Quantum Standards:
The National Institute of Standards and Technology (NIST) has selected standardized quantum-resistant algorithms:
- ML-KEM (originally CRYSTALS-Kyber): A lattice-based Module Learning-with-Errors algorithm for general public key encryption and key encapsulation mechanism (KEM).
- ML-DSA (originally CRYSTALS-Dilithium): A lattice-based signature scheme designed for digital signatures and authentication.
- SLH-DSA (originally SPHINCS+): A stateless hash-based signature scheme providing an alternative fallback without relying on lattice hardness assumptions.

4. Transition Roadmap:
Enterprise and government organizations are mandated to catalog all cryptographic assets and implement hybrid classical-PQC encryption protocols by 2026, targeting complete deprecation of legacy RSA and ECC by 2030.
            """.trimIndent()
        ),
        SampleDoc(
            title = "NASA Europa Clipper Science & Mission Profile",
            category = "Science",
            content = """
NASA Europa Clipper Mission Overview and Planetary Habitability Investigation.

1. Mission Objectives:
The Europa Clipper spacecraft is designed to investigate Jupiter's icy moon Europa to determine whether places below its icy crust could support life. Planetary scientists have strong evidence that Europa harbors a global subsurface liquid saltwater ocean containing more water than all of Earth's oceans combined, in direct contact with a rocky mantle.

2. Spacecraft Trajectory & Radiation Strategy:
Europa Clipper avoids the lethal radiation environment near Jupiter by placing the spacecraft in an elongated orbit around Jupiter, performing nearly 50 close flybys of Europa at altitudes ranging from 25 to 100 kilometers above the surface. The spacecraft utilized a Mars-Earth Gravity Assist (MEGA) trajectory to reach the Jovian system.

3. Scientific Instruments:
- REASON (Radar for Europa Assessment and Sounding: Ocean to Near-surface): An ice-penetrating radar that sounds Europa's icy shell to detect the ocean interface and internal water pockets.
- SUDA (Surface Dust Analyzer): An instrument that samples and chemically identifies tiny ice particles ejected from Europa's surface by micrometeorite impacts.
- MASPEX (Mass Spectrometer for Planetary Exploration): Analyzes trace gases in Europa's thin atmosphere and potential plume vapors to identify organic compounds and volatiles.
- Europa Imaging System (EIS): High-resolution narrow-angle and wide-angle cameras mapping Europa's surface fractures, chaos terrain, and tectonic ridges at half-meter resolution.
            """.trimIndent()
        )
    )
}

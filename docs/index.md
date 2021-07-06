# About

**STMonitor: Session Types Monitor Synthesiser**

Given a session type _S_, the tool synthesises the Scala code of a type-checked monitor (based on the library [lchannels](https://github.com/alcestes/lchannels)). The monitor verifies at runtime whether the interactions between a client and server follow the protocol _S_.

The approach and its underlying theory are described in the following papers:

* Christian Bartolo Burlò, Adrian Francalanza and Alceste Scalas. *[Towards a Hybrid Verification Methodology for Communication Protocols (Short Paper)](https://doi.org/10.1007/978-3-030-50086-3_13)*. FORTE 2020. ([Pesentation video](https://youtu.be/FL_teSjllSE))

* Christian Bartolo Burlò, Adrian Francalanza, Alceste Scalas, Catia Trubiani and Emilio Tuosto. *[Towards Probabilistic Session-Type Monitoring](https://link.springer.com/chapter/10.1007/978-3-030-78142-2_7)*. COORDINATION 2021. ([Pesentation video](https://www.youtube.com/watch?v=7ncHqpgTjjc) and [Tool demo](https://www.youtube.com/watch?v=_NlaNk6nphQ)) **NOTE:** *to access the implementation of the probabilistic monitors switch branch to `pstmonitor`.*

* Christian Bartolo Burlò, Adrian Francalanza and Alceste Scalas. *On the Monitorability of Session Types, in Theory and Practice*. ECOOP 2021 (to appear).

      <img src="/images/artifacts_available_v1_1.png" width="80" height="80"> <img src="/images/artifacts_evaluated_functional_v1_1.png" width="80" height="80"> <img src="/images/artifacts_evaluated_reusable_v1_1.png" width="80" height="80">

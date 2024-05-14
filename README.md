# SundaeSwap Scooper Analytics

This is a simple crawler app that stores relevant scoop transaction info and serves aggreagate data over a rest endpoint.

This project can run w/o any dependencies or infrastructure by leveraging publicly available nodes, like the IOG one, but 
it will be very slow.

Please check requirements below to run in a more performant way and how to make it resilient to restarts.

## Requirements

In order to be resilient to restarts and as fast as possible you require:
1. a blockfrost api key (or your own blockfrost)
2. a cardano mainnet node (not the socket, just a node)

You can set the stuff above via the following env vars:

```bash
CARDANO_NODE_HOST: my_node_host
CARDANO_NODE_PORT: 3000
BLOCKFROST_KEY: my_project_key
```
The reason behind blocksfrost is to compute at restarts which block the last persisted tx belongs to, and the indexer
will restart from that very same block, instead of rescanning blocks since launch of the v3 protocol.


## Docker Compose

Docker compose can be used to run the project.

Ensure the `docker-compose.yaml` is set with your preferred configuration via environmental variable, then issue:

`docker compose up -d`

**NOTE:** Docker compose will not check whether you have the latest version running, so if you pull more updates from
github, you will need to rebuild the image via `docker compose build`, this will rebuild the image that will eventually
be run in docker compose.

The `api` will be available at `http://localhost:8080`

## API

There currently are 2 endpoints:
* list of ordered scoops: `/scoops` with 2 query params: `slot` and `limit` that can be used to paginate. 
Scoops are returned sorted by `slot` descending.
* basic stats endpoint `/scoops/stats`

### Paginated List

`curl http://localhost:8080/scoops?slot=124132996&limit=5`

Example Response:

```json
[
  {
    "txHash": "3e0b3d23d9b2cfad90b63326bfce55e1d51708a4df7e4ef6b341446317a6bf61",
    "scooperPubKeyHash": "8ca0e08cdbc30fa0dd21833d7370d666493ecc28b136df179f97fb5d",
    "orders": 1,
    "fees": 340573,
    "slot": 124132822,
    "epoch": 0,
    "version": 3
  },
  {
    "txHash": "8a76477501af0b434eb8fefad9c758ddb45fd6b112fdb838cf29b44618cf5fd0",
    "scooperPubKeyHash": "8ca0e08cdbc30fa0dd21833d7370d666493ecc28b136df179f97fb5d",
    "orders": 2,
    "fees": 389118,
    "slot": 124132362,
    "epoch": 0,
    "version": 3
  },
  {
    "txHash": "8d96b7a35c499394bfac933ab1d8bde9fe625047b289a28be295ff964a699bb2",
    "scooperPubKeyHash": "8ca0e08cdbc30fa0dd21833d7370d666493ecc28b136df179f97fb5d",
    "orders": 1,
    "fees": 370841,
    "slot": 124132347,
    "epoch": 0,
    "version": 3
  },
  {
    "txHash": "26a9802d941b037fa26d1c90089e483c4dc0f43054c05d1b39bdc0b13c012de5",
    "scooperPubKeyHash": "570cd6294587645d26c690a72d40fede1e7a28cb3ddc78ff76655820",
    "orders": 1,
    "fees": 333786,
    "slot": 124132290,
    "epoch": 0,
    "version": 3
  },
  {
    "txHash": "e7e4eb38193c0147a3ef58a30b4e88a9d31fc96a84ca8827ddd5069e11f73ee2",
    "scooperPubKeyHash": "37eb116b3ff8a70e4be778b5e8d30d3b40421ffe6622f6a983f67f3f",
    "orders": 1,
    "fees": 344722,
    "slot": 124132151,
    "epoch": 0,
    "version": 3
  }
]
```

### Scooper Statistics

`curl http://localhost:8080/scoops/stats`

Result will be similar to 
```json
[
  {
    "totalScoops": 666,
    "pubKeyHash": "8ca0e08cdbc30fa0dd21833d7370d666493ecc28b136df179f97fb5d",
    "totalOrders": 1027,
    "totalFees": 244362918
  },
  {
    "totalScoops": 521,
    "pubKeyHash": "37eb116b3ff8a70e4be778b5e8d30d3b40421ffe6622f6a983f67f3f",
    "totalOrders": 700,
    "totalFees": 188546219
  },
  {
    "totalScoops": 315,
    "pubKeyHash": "570cd6294587645d26c690a72d40fede1e7a28cb3ddc78ff76655820",
    "totalOrders": 454,
    "totalFees": 112226135
  },
  {
    "totalScoops": 91,
    "pubKeyHash": "6510a3ec0a6f273e31acc82f9f2ffb089413549a04149ea37ef8d33b",
    "totalOrders": 124,
    "totalFees": 32585791
  },
  {
    "totalScoops": 15,
    "pubKeyHash": "dd8a02814820616b137e0fb4852fd8aab36875d849919ca68aa6cb70",
    "totalOrders": 25,
    "totalFees": 5612435
  },
  {
    "totalScoops": 13,
    "pubKeyHash": "61f1baeda28f3f83413b92a7d28d2f7b545d718f2f28f971b92b3a21",
    "totalOrders": 22,
    "totalFees": 4788772
  },
  {
    "totalScoops": 7,
    "pubKeyHash": "fe9315a8d1f638a4836e9ec396d43e1f6ba88e45a7f5a5e37a77071a",
    "totalOrders": 10,
    "totalFees": 2500192
  },
  {
    "totalScoops": 3,
    "pubKeyHash": "baec408a6fedd39ac0404a2f82c6e75ef06659d8596f9d0af6e01241",
    "totalOrders": 4,
    "totalFees": 1083901
  },
  {
    "totalScoops": 3,
    "pubKeyHash": "ee8ed5ef92d0a51c6962aac7012906d280aeb412900a7621f782c7c9",
    "totalOrders": 4,
    "totalFees": 1083982
  },
  {
    "totalScoops": 2,
    "pubKeyHash": "9366b01d6baf040245ee07127fc8af4f04a75b91c6a97f69c7f6463a",
    "totalOrders": 3,
    "totalFees": 725652
  },
  {
    "totalScoops": 2,
    "pubKeyHash": "a14cb1a14c4b5810a21103e389f4abdbdec010b766e2dc329a4e0e96",
    "totalOrders": 4,
    "totalFees": 755726
  },
  {
    "totalScoops": 1,
    "pubKeyHash": "cba4b71bd8cecc54c526bcd71da84f6f79e568604e574149854dbb86",
    "totalOrders": 2,
    "totalFees": 388832
  },
  {
    "totalScoops": 1,
    "pubKeyHash": "40282b949abda48a573fe2757971a1369d2674ac9b6d98c1c2bdbdf7",
    "totalOrders": 2,
    "totalFees": 390851
  },
  {
    "totalScoops": 1,
    "pubKeyHash": "53d6b12089d642d3bfdc61d5f0f3fddfeeb56f55dcd5bd796b5c25a1",
    "totalOrders": 2,
    "totalFees": 388687
  },
  {
    "totalScoops": 1,
    "pubKeyHash": "6c8ecf30ba1a025dd324cb0598c8ff87522b324901299cf3f4f1d0b2",
    "totalOrders": 2,
    "totalFees": 395671
  },
  {
    "totalScoops": 1,
    "pubKeyHash": "7a7a02beabb674125d734a24817aea9505b9113540cc72f4ef5c2faf",
    "totalOrders": 2,
    "totalFees": 391296
  },
  {
    "totalScoops": 1,
    "pubKeyHash": "251f7fb11f84f81653ee5b76a10dd29fa36ec7717aafe689490cb7e4",
    "totalOrders": 2,
    "totalFees": 382203
  }
]
```

## PSQL

Init local dev psql db

`createuser --superuser postgres`

`psql -U postgres`

Then create db:

```
CREATE USER sundaeswap PASSWORD 'password';

CREATE DATABASE sundaeswap_scooper WITH OWNER sundaeswap;
```


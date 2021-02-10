def get_stats(results):
    for gen in results:
        total = 0
        m = 0
        for score in results[gen]:
            total += results[gen][score]
            m = max(m, results[gen][score])
        total /= len(results[gen])

        print (gen, total, m)

if __name__ == '__main__':

    x={}
    get_stats(x)
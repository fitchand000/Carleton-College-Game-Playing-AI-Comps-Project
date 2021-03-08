def get_stats(results):
    """
    :param results: dictionary output from a trainer
    """
    for gen in results:
        total = 0
        m = 0
        for score in results[gen]:
            total += results[gen][score]
            m = max(m, results[gen][score])
        total /= len(results[gen])

        print (gen, total, m)

if __name__ == '__main__':

    pass